package com.duke.knowledgeqa.vo;

import lombok.*;

@Data
@Builder
public class CollectionInfoVO {
    private String collectionName = "";
    private String status = "";
    private long vectorSize = 0;
    private String distance = "";
}
