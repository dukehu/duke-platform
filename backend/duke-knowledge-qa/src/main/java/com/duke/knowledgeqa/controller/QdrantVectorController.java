package com.duke.knowledgeqa.controller;

import com.duke.framework.common.Result;
import com.duke.knowledgeqa.dto.VectorParamsDTO;
import com.duke.knowledgeqa.service.IQdrantVectorService;
import com.duke.knowledgeqa.vo.CollectionInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Qdrant CURD")
@RestController
@RequestMapping("/qdrant")
@RequiredArgsConstructor
public class QdrantVectorController {

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
    public Result<CollectionInfoVO> upsert(@RequestBody Map<String, String> params) {
        iQdrantVectorService.insertVector(params.get("collectionName"), params.get("text"));
        return Result.success();
    }
}
