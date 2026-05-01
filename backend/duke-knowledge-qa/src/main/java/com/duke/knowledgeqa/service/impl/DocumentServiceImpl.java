package com.duke.knowledgeqa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duke.framework.common.PageResult;
import com.duke.framework.exception.BusinessException;
import com.duke.knowledgeqa.dto.DocumentQueryDTO;
import com.duke.knowledgeqa.entity.Document;
import com.duke.knowledgeqa.mapper.DocumentMapper;
import com.duke.knowledgeqa.service.IDocumentService;
import com.duke.knowledgeqa.vo.DocumentVO;
import com.duke.knowledgeqa.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements IDocumentService {

    private final DocumentMapper documentMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url:}")
    private String baseUrl;

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(
            Arrays.asList("pdf", "txt", "md", "docx", "doc")
    );
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    @Override
    public UploadResultVO upload(MultipartFile file, String category, String tags, Long userId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("文件不能为空");
            }

            String originalFileName = file.getOriginalFilename();
            if (!StringUtils.hasText(originalFileName)) {
                throw new BusinessException("文件名无效");
            }

            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            if (!ALLOWED_TYPES.contains(fileExtension)) {
                throw new BusinessException("不支持的文件类型，仅支持: pdf, txt, md, docx, doc");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException("文件大小超过 100MB");
            }

            // 计算文件 MD5
            String fileMd5 = calculateFileMd5(file);

            // 检查 MD5 是否已存在
            Document existingDoc = documentMapper.selectOne(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getFileMd5, fileMd5)
                            .eq(Document::getDeleted, 0)
            );
            if (existingDoc != null) {
                log.info("文件已存在: md5={}, docId={}, fileName={}", fileMd5, existingDoc.getId(), originalFileName);
                return UploadResultVO.builder()
                        .documentId(existingDoc.getId())
                        .status(existingDoc.getStatus())
                        .build();
            }

            // 生成 UUID 文件名
            String uuidFileName = UUID.randomUUID() + "." + fileExtension;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(uuidFileName);
            file.transferTo(filePath.toFile());

            // 构造相对 URL 路径
            String fileUrl = "/api/knowledge-qa/files/" + uuidFileName;

            // 提取标题（去除扩展名）
            String title = originalFileName.substring(0, originalFileName.lastIndexOf('.'));

            // 验证并保存标签（确保是有效的 JSON 数组）
            String finalTags = validateTags(tags);

            // 创建文档记录
            Document document = Document.builder()
                    .title(title)
                    .category(StringUtils.hasText(category) ? category : "")
                    .tags(finalTags)
                    .fileType(fileExtension)
                    .fileUrl(fileUrl)
                    .fileName(originalFileName)
                    .fileSize(file.getSize())
                    .fileMd5(fileMd5)
                    .status("DRAFT")
                    .createdBy(userId)
                    .deleted(0)
                    .build();

            documentMapper.insert(document);

            log.info("文档上传成功: id={}, fileName={}, fileUrl={}, tags={}", document.getId(), originalFileName, fileUrl, finalTags);

            return UploadResultVO.builder()
                    .documentId(document.getId())
                    .status("DRAFT")
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    private String validateTags(String tags) {
        try {
            if (!StringUtils.hasText(tags) || tags.equals("[]")) {
                return "[]";
            }
            // 验证是否是有效的 JSON 数组
            com.alibaba.fastjson2.JSONArray.parse(tags);
            return tags;
        } catch (Exception e) {
            log.warn("标签格式错误，使用空数组: {}", tags);
            return "[]";
        }
    }

    @Override
    public PageResult<DocumentVO> page(DocumentQueryDTO dto) {
        try {
            // 确保分页参数有效
            long current = (dto.getCurrent() != null && dto.getCurrent() > 0) ? dto.getCurrent() : 1L;
            long size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : 10L;

            log.info("分页查询: current={}, size={}, category={}, keyword={}, status={}",
                    current, size, dto.getCategory(), dto.getKeyword(), dto.getStatus());

            LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                    .eq(StringUtils.hasText(dto.getCategory()), Document::getCategory, dto.getCategory())
                    .like(StringUtils.hasText(dto.getKeyword()), Document::getTitle, dto.getKeyword())
                    .eq(StringUtils.hasText(dto.getStatus()), Document::getStatus, dto.getStatus())
                    .orderByDesc(Document::getCreateTime);

            // 先计算总数
            long total = documentMapper.selectCount(wrapper);

            // 再查询分页数据
            long offset = (current - 1) * size;
            List<Document> records = documentMapper.selectList(wrapper.last("LIMIT " + offset + ", " + size));

            List<DocumentVO> vos = records.stream()
                    .map(DocumentVO::fromEntity)
                    .collect(Collectors.toList());

            log.info("分页查询结果: total={}, records={}", total, vos.size());

            return PageResult.of(total, vos);
        } catch (Exception e) {
            log.error("文档列表查询失败", e);
            throw new BusinessException("查询文档列表失败: " + e.getMessage());
        }
    }

    @Override
    public DocumentVO getById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new BusinessException("文档 ID 无效");
            }
            Document document = documentMapper.selectById(id);
            if (document == null) {
                throw new BusinessException("文档不存在");
            }
            return DocumentVO.fromEntity(document);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询文档失败: id={}", id, e);
            throw new BusinessException("查询文档失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new BusinessException("文档 ID 无效");
            }
            int result = documentMapper.deleteById(id);
            if (result <= 0) {
                throw new BusinessException("文档不存在");
            }
            log.info("文档删除成功: id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除文档失败: id={}", id, e);
            throw new BusinessException("删除文档失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String calculateFileMd5(MultipartFile file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
