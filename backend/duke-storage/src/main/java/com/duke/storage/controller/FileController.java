package com.duke.storage.controller;

import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.storage.dto.ChunkMergeDTO;
import com.duke.storage.dto.ChunkUploadDTO;
import com.duke.storage.dto.FileQueryDTO;
import com.duke.storage.entity.SysFile;
import com.duke.storage.service.IFileStorageService;
import com.duke.storage.vo.ChunkCheckVO;
import com.duke.storage.vo.FileCheckVO;
import com.duke.storage.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理控制器
 */
@Slf4j
@Tag(name = "文件管理")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    
    private final IFileStorageService fileStorageService;
    
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        FileVO fileVO = fileStorageService.uploadFile(file);
        return Result.success(fileVO);
    }
    
    @Operation(summary = "校验文件是否存在（秒传）")
    @GetMapping("/check/exist")
    public Result<FileCheckVO> checkExist(
            @RequestParam String fileName,
            @RequestParam Long fileSize) {
        FileCheckVO result = fileStorageService.checkFileExists(fileName, fileSize);
        return Result.success(result);
    }
    
    @Operation(summary = "上传分片")
    @PostMapping("/chunk/upload")
    public Result<Void> uploadChunk(
            ChunkUploadDTO dto,
            @RequestParam("chunk") MultipartFile chunk) {
        try {
            fileStorageService.uploadChunk(dto, chunk.getInputStream());
            return Result.success();
        } catch (Exception e) {
            log.error("分片上传失败", e);
            return Result.fail("分片上传失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "检查分片上传状态")
    @GetMapping("/chunk/check")
    public Result<ChunkCheckVO> checkChunks(
            @RequestParam String chunkId,
            @RequestParam Integer chunkTotal) {
        ChunkCheckVO result = fileStorageService.checkChunks(chunkId, chunkTotal);
        return Result.success(result);
    }
    
    @Operation(summary = "合并分片")
    @PostMapping("/chunk/merge")
    public Result<FileVO> mergeChunks(@RequestBody ChunkMergeDTO dto) {
        FileVO fileVO = fileStorageService.mergeChunks(dto);
        return Result.success(fileVO);
    }
    
    @Operation(summary = "获取文件信息")
    @GetMapping("/{fileId}")
    public Result<FileVO> getById(@PathVariable Long fileId) {
        FileVO fileVO = fileStorageService.getFileById(fileId);
        return Result.success(fileVO);
    }
    
    @Operation(summary = "分页查询文件列表")
    @GetMapping("/list")
    public Result<PageResult<FileVO>> pageFiles(FileQueryDTO dto) {
        PageResult<FileVO> result = fileStorageService.pageFiles(dto);
        return Result.success(result);
    }
    
    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        fileStorageService.deleteFile(fileId);
        return Result.success();
    }
    
    @Operation(summary = "下载文件")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) {
        SysFile sysFile = fileStorageService.getFileEntity(fileId);
        InputStream inputStream = fileStorageService.getFileInputStream(fileId);
        
        String encodedFileName = URLEncoder.encode(sysFile.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFileName)
                .body(new InputStreamResource(inputStream));
    }
    
    @Operation(summary = "预览文件")
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long fileId) {
        SysFile sysFile = fileStorageService.getFileEntity(fileId);
        InputStream inputStream = fileStorageService.getFileInputStream(fileId);
        
        MediaType mediaType = MediaType.parseMediaType(sysFile.getMimeType());
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(inputStream));
    }
}
