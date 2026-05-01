package com.duke.knowledgeqa.controller;

import com.duke.framework.common.PageResult;
import com.duke.framework.common.Result;
import com.duke.knowledgeqa.dto.DocumentQueryDTO;
import com.duke.knowledgeqa.service.IDocumentService;
import com.duke.knowledgeqa.vo.DocumentVO;
import com.duke.knowledgeqa.vo.UploadResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "文档管理")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public Result<UploadResultVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "") String category,
            @RequestParam(value = "tags", defaultValue = "[]") String tags,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(documentService.upload(file, category, tags, userId));
    }

    @Operation(summary = "分页查询文档列表")
    @GetMapping
    public Result<PageResult<DocumentVO>> page(DocumentQueryDTO dto) {
        return Result.success(documentService.page(dto));
    }

    @Operation(summary = "按 ID 查询文档")
    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        return Result.success(documentService.getById(id));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }
}
