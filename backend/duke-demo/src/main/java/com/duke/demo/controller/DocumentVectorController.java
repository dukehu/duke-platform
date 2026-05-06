package com.duke.demo.controller;

import com.duke.demo.service.IDocumentPipelineService;
import com.duke.demo.service.IQdrantVectorService;
import com.duke.demo.vo.DocumentProcessResult;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文档向量处理 API
 */
@Slf4j
@Tag(name = "文档向量处理")
@RestController
@RequestMapping("/document-vector")
@RequiredArgsConstructor
public class DocumentVectorController {

    private final IDocumentPipelineService documentPipelineService;

    @Operation(summary = "上传文档并全流程处理（解析→分块→向量化→存入Qdrant）")
    @PostMapping(value = "/upload-and-process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DocumentProcessResult> uploadAndProcess(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "collectionName", required = false) String collectionName
    ) throws IOException {
        log.info("Uploading and processing document: {}", file.getOriginalFilename());
        DocumentProcessResult result = documentPipelineService.processDocument(file, collectionName);
        return Result.success(result);
    }

    @Operation(summary = "语义搜索")
    @GetMapping("/search")
    public Result<List<IQdrantVectorService.SearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "documents") String collectionName,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "0.5") float scoreThreshold
    ) {
        log.info("Searching for: '{}' in collection: {}", query, collectionName);
        List<IQdrantVectorService.SearchResult> results = documentPipelineService.search(query, collectionName, limit, scoreThreshold);
        return Result.success(results);
    }
}
