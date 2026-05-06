package com.duke.storage.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duke.framework.common.PageResult;
import com.duke.framework.exception.BusinessException;
import com.duke.storage.config.properties.FileProperties;
import com.duke.storage.config.properties.MinioProperties;
import com.duke.storage.dto.ChunkMergeDTO;
import com.duke.storage.dto.ChunkUploadDTO;
import com.duke.storage.dto.FileQueryDTO;
import com.duke.storage.entity.SysFile;
import com.duke.storage.entity.SysFileChunk;
import com.duke.storage.enums.StorageModeEnum;
import com.duke.storage.mapper.SysFileChunkMapper;
import com.duke.storage.mapper.SysFileMapper;
import com.duke.storage.service.IFileStorageService;
import com.duke.storage.util.Md5Util;
import com.duke.storage.vo.ChunkCheckVO;
import com.duke.storage.vo.FileCheckVO;
import com.duke.storage.vo.FileVO;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件存储服务实现（MinIO存储）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enable", havingValue = "true")
public class MinioFileStorageServiceImpl implements IFileStorageService {
    
    private final SysFileMapper sysFileMapper;
    private final SysFileChunkMapper sysFileChunkMapper;
    private final FileProperties fileProperties;
    private final MinioProperties minioProperties;
    private MinioClient minioClient;
    
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
            
            // 确保 bucket 存在
            boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build()
            );
            if (!found) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build()
                );
                log.info("创建MinIO Bucket: {}", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("初始化MinIO客户端失败", e);
            throw new RuntimeException("初始化MinIO客户端失败", e);
        }
    }
    
    @Override
    public FileVO uploadFile(MultipartFile file) {
        try {
            // 1. 校验文件
            validateFile(file);
            
            // 2. 计算MD5并检查秒传
            String md5 = Md5Util.calculateMd5(file.getInputStream());
            SysFile existFile = sysFileMapper.selectOne(
                new LambdaQueryWrapper<SysFile>()
                    .eq(SysFile::getFileMd5, md5)
                    .eq(SysFile::getDeleted, 0)
            );
            
            if (existFile != null) {
                log.info("文件秒传: md5={}, fileId={}", md5, existFile.getId());
                return convertToVO(existFile);
            }
            
            // 3. 上传到MinIO
            String originalName = file.getOriginalFilename();
            String suffix = getFileSuffix(originalName);
            String objectName = generateObjectName(suffix);
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // 4. 生成访问URL
            String fileUrl = generateFileUrl(objectName);
            
            // 5. 保存到数据库
            SysFile sysFile = SysFile.builder()
                .originalName(originalName)
                .fileSuffix(suffix)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .storageMode(StorageModeEnum.MINIO.getCode())
                .savePath(objectName)
                .fileUrl(fileUrl)
                .fileMd5(md5)
                .build();
            
            sysFileMapper.insert(sysFile);
            
            log.info("文件上传成功(MinIO): id={}, name={}, size={}", sysFile.getId(), originalName, file.getSize());
            return convertToVO(sysFile);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败(MinIO)", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public FileCheckVO checkFileExists(String fileName, Long fileSize) {
        return FileCheckVO.builder()
            .exists(false)
            .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(ChunkUploadDTO dto, InputStream chunkData) {
        try {
            // 1. 校验分片信息
            validateChunk(dto);
            
            // 2. 生成分片对象名
            String objectName = generateChunkObjectName(dto.getChunkId(), dto.getChunkIndex());
            
            // 3. 上传分片到MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(chunkData, dto.getChunkSize(), -1)
                    .build()
            );
            
            // 4. 记录分片信息到数据库
            SysFileChunk chunk = SysFileChunk.builder()
                .chunkId(dto.getChunkId())
                .fileName(dto.getFileName())
                .fileSuffix(getFileSuffix(dto.getFileName()))
                .chunkIndex(dto.getChunkIndex())
                .chunkTotal(dto.getChunkTotal())
                .chunkSize(dto.getChunkSize())
                .fileSize(dto.getFileSize())
                .chunkPath(objectName)
                .storageMode(StorageModeEnum.MINIO.getCode())
                .build();
            
            sysFileChunkMapper.insert(chunk);
            
            log.info("分片上传成功(MinIO): chunkId={}, index={}/{}", 
                dto.getChunkId(), dto.getChunkIndex(), dto.getChunkTotal());
                
        } catch (Exception e) {
            log.error("分片上传失败(MinIO)", e);
            throw new BusinessException("分片上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public ChunkCheckVO checkChunks(String chunkId, Integer chunkTotal) {
        List<SysFileChunk> chunks = sysFileChunkMapper.selectList(
            new LambdaQueryWrapper<SysFileChunk>()
                .eq(SysFileChunk::getChunkId, chunkId)
        );
        
        List<Integer> uploadedIndexes = chunks.stream()
            .map(SysFileChunk::getChunkIndex)
            .collect(Collectors.toList());
        
        boolean completed = uploadedIndexes.size() == chunkTotal;
        
        return ChunkCheckVO.builder()
            .uploadedChunks(uploadedIndexes)
            .completed(completed)
            .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO mergeChunks(ChunkMergeDTO dto) {
        try {
            // 1. 查询所有分片
            List<SysFileChunk> chunks = sysFileChunkMapper.selectList(
                new LambdaQueryWrapper<SysFileChunk>()
                    .eq(SysFileChunk::getChunkId, dto.getChunkId())
                    .orderByAsc(SysFileChunk::getChunkIndex)
            );
            
            if (chunks.size() != dto.getChunkTotal()) {
                throw new BusinessException("分片不完整，无法合并");
            }
            
            // 2. 创建临时文件用于合并和计算MD5
            Path tempFile = Files.createTempFile("merge_", "_" + dto.getFileName());
            
            try (OutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                for (SysFileChunk chunk : chunks) {
                    // 从MinIO下载分片
                    InputStream chunkStream = minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(chunk.getChunkPath())
                            .build()
                    );
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = chunkStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    chunkStream.close();
                }
            }
            
            // 3. 计算完整文件MD5
            String md5;
            try (InputStream inputStream = new FileInputStream(tempFile.toFile())) {
                md5 = Md5Util.calculateMd5(inputStream);
            }
            
            // 4. 检查是否已存在（秒传）
            SysFile existFile = sysFileMapper.selectOne(
                new LambdaQueryWrapper<SysFile>()
                    .eq(SysFile::getFileMd5, md5)
                    .eq(SysFile::getDeleted, 0)
            );
            
            if (existFile != null) {
                // 删除临时文件和MinIO中的分片
                Files.deleteIfExists(tempFile);
                deleteChunksFromMinio(chunks);
                deleteChunks(dto.getChunkId());
                log.info("分片合并后秒传(MinIO): md5={}, fileId={}", md5, existFile.getId());
                return convertToVO(existFile);
            }
            
            // 5. 上传合并后的文件到MinIO
            String suffix = getFileSuffix(dto.getFileName());
            String objectName = generateObjectName(suffix);
            
            try (InputStream inputStream = new FileInputStream(tempFile.toFile())) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .stream(inputStream, dto.getFileSize(), -1)
                        .contentType(getMimeType(suffix))
                        .build()
                );
            }
            
            // 删除临时文件
            Files.deleteIfExists(tempFile);
            
            // 6. 生成访问URL
            String fileUrl = generateFileUrl(objectName);
            
            // 7. 保存到数据库
            SysFile sysFile = SysFile.builder()
                .originalName(dto.getFileName())
                .fileSuffix(suffix)
                .fileSize(dto.getFileSize())
                .mimeType(getMimeType(suffix))
                .storageMode(StorageModeEnum.MINIO.getCode())
                .savePath(objectName)
                .fileUrl(fileUrl)
                .fileMd5(md5)
                .build();
            
            sysFileMapper.insert(sysFile);
            
            // 8. 删除分片记录和MinIO中的分片
            deleteChunksFromMinio(chunks);
            deleteChunks(dto.getChunkId());
            
            log.info("分片合并成功(MinIO): id={}, name={}, md5={}", sysFile.getId(), dto.getFileName(), md5);
            return convertToVO(sysFile);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分片合并失败(MinIO)", e);
            throw new BusinessException("分片合并失败: " + e.getMessage());
        }
    }
    
    @Override
    public FileVO getFileById(Long fileId) {
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null) {
            throw new BusinessException("文件不存在");
        }
        return convertToVO(sysFile);
    }
    
    @Override
    public PageResult<FileVO> pageFiles(FileQueryDTO dto) {
        long current = (dto.getCurrent() != null && dto.getCurrent() > 0) ? dto.getCurrent() : 1L;
        long size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : 10L;
        
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
            .eq(SysFile::getDeleted, 0)
            .like(StrUtil.isNotBlank(dto.getKeyword()), 
                  SysFile::getOriginalName, dto.getKeyword())
            .eq(StrUtil.isNotBlank(dto.getFileType()), 
                SysFile::getFileSuffix, dto.getFileType())
            .orderByDesc(SysFile::getCreateTime);
        
        long total = sysFileMapper.selectCount(wrapper);
        
        long offset = (current - 1) * size;
        List<SysFile> records = sysFileMapper.selectList(wrapper.last("LIMIT " + offset + ", " + size));
        
        List<FileVO> vos = records.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResult.of(total, vos);
    }
    
    @Override
    public void deleteFile(Long fileId) {
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null) {
            throw new BusinessException("文件不存在");
        }
        
        // 逻辑删除
        sysFileMapper.deleteById(fileId);
        log.info("文件逻辑删除(MinIO): id={}", fileId);
    }
    
    @Override
    public InputStream getFileInputStream(Long fileId) {
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null) {
            throw new BusinessException("文件不存在");
        }
        
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(sysFile.getSavePath())
                    .build()
            );
        } catch (Exception e) {
            log.error("获取文件流失败(MinIO)", e);
            throw new BusinessException("获取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public SysFile getFileEntity(Long fileId) {
        return sysFileMapper.selectById(fileId);
    }
    
    // ==================== 私有方法 ====================
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        String originalName = file.getOriginalFilename();
        String suffix = getFileSuffix(originalName).toLowerCase();
        
        List<String> allowedSuffixes = List.of(fileProperties.getAllowSuffix().split(","));
        if (!allowedSuffixes.contains(suffix)) {
            throw new BusinessException("不支持的文件类型: " + suffix);
        }
        
        long maxSize = fileProperties.getMaxFileSize() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过 " + fileProperties.getMaxFileSize() + "MB");
        }
    }
    
    private void validateChunk(ChunkUploadDTO dto) {
        if (StrUtil.isBlank(dto.getChunkId())) {
            throw new BusinessException("分片ID不能为空");
        }
        if (dto.getChunkIndex() == null || dto.getChunkIndex() < 1) {
            throw new BusinessException("分片序号无效");
        }
        if (dto.getChunkTotal() == null || dto.getChunkTotal() < 1) {
            throw new BusinessException("总分片数无效");
        }
    }
    
    private String getFileSuffix(String fileName) {
        if (StrUtil.isBlank(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    private String generateObjectName(String suffix) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = System.currentTimeMillis() + "_" + System.nanoTime() + "." + suffix;
        return datePath + "/" + fileName;
    }
    
    private String generateChunkObjectName(String chunkId, Integer chunkIndex) {
        return "chunks/" + chunkId + "/chunk_" + chunkIndex;
    }
    
    private String generateFileUrl(String objectName) {
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
    }
    
    private String getMimeType(String suffix) {
        return switch (suffix.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
    
    private FileVO convertToVO(SysFile sysFile) {
        return FileVO.builder()
            .id(sysFile.getId())
            .originalName(sysFile.getOriginalName())
            .fileSuffix(sysFile.getFileSuffix())
            .fileSize(sysFile.getFileSize())
            .formattedSize(formatFileSize(sysFile.getFileSize()))
            .mimeType(sysFile.getMimeType())
            .storageMode(sysFile.getStorageMode())
            .fileUrl(sysFile.getFileUrl())
            .fileMd5(sysFile.getFileMd5())
            .createTime(sysFile.getCreateTime())
            .build();
    }
    
    private String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size.doubleValue();
        
        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }
    
    private void deleteChunks(String chunkId) {
        sysFileChunkMapper.delete(
            new LambdaQueryWrapper<SysFileChunk>()
                .eq(SysFileChunk::getChunkId, chunkId)
        );
    }
    
    private void deleteChunksFromMinio(List<SysFileChunk> chunks) {
        for (SysFileChunk chunk : chunks) {
            try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(chunk.getChunkPath())
                        .build()
                );
            } catch (Exception e) {
                log.warn("删除MinIO分片失败: {}", chunk.getChunkPath(), e);
            }
        }
    }
}
