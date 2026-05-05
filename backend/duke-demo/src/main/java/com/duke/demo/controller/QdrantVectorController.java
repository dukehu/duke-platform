package com.duke.demo.controller;

import com.duke.demo.dto.VectorParamsDTO;
import com.duke.demo.service.IQdrantVectorService;
import com.duke.demo.util.QwenEmbeddingUtil;
import com.duke.demo.vo.CollectionInfoVO;
import com.duke.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "手写 Qdrant CURD")
@RestController
@RequestMapping("/qdrant")
@RequiredArgsConstructor
public class QdrantVectorController {

    private final QwenEmbeddingUtil qwenEmbeddingUtil;
    private final IQdrantVectorService iQdrantVectorService;

    @Operation(summary = "根据集合名称获取")
    @GetMapping("/listByCollectionName/{collectionName}")
    public Result<CollectionInfoVO> listByCollectionName(@PathVariable String collectionName) {
        return Result.success(iQdrantVectorService.listCollection(collectionName));
    }

    @Operation(summary = "新增集合")
    @PostMapping("/createCollection")
    public Result<CollectionInfoVO> createCollection(@RequestBody @Valid VectorParamsDTO params) {
        return Result.success(iQdrantVectorService.createCollection(params));
    }

    @Operation(summary = "新增数据")
    @PostMapping("/upsert")
    @ResponseBody
    public Result<CollectionInfoVO> upsert(@RequestBody Map<String, String> params) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docId", "uuid-111");
        metadata.put("fileName", "手册.pdf");
        metadata.put("title", "Spring Boot 原理");
        metadata.put("chunkIndex", 0);
        metadata.put("page", 5);
        iQdrantVectorService.insertVector(
                params.get("collectionName"),
                params.get("text"),
                qwenEmbeddingUtil.textToVector(params.get("text")),
                metadata
        );
        return Result.success();
    }

    @Operation(summary = "查询数据")
    @PostMapping("/search")
    public Result<List<IQdrantVectorService.SearchResult>> search(@RequestBody Map<String, String> params) {
        return Result.success(iQdrantVectorService.search(
                params.get("collectionName"),
                qwenEmbeddingUtil.textToVector(params.get("text")),
                2,
                0.5f
        ));
    }
}
