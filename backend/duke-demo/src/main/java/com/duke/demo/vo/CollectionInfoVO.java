package com.duke.demo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionInfoVO {
    private String collectionName = "";
    private String status = "";
    private long vectorSize = 0;
    private String distance = "";
}
