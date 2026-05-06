package com.duke.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VectorParamsDTO {

    @NotBlank(message = "集合名称不能为空")
    private String collectionName;

    private Integer vectorSize = 1024;

}
