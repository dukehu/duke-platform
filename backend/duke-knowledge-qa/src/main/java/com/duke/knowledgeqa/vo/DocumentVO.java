package com.duke.knowledgeqa.vo;

import com.alibaba.fastjson2.JSON;
import com.duke.knowledgeqa.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVO {

    private Long id;

    private String title;

    private String category;

    private List<String> tags;

    private String fileType;

    private String fileUrl;

    private String status;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static DocumentVO fromEntity(Document entity) {
        if (entity == null) {
            return null;
        }
        List<String> tags = Collections.emptyList();
        if (entity.getTags() != null && !entity.getTags().isEmpty() && !entity.getTags().equals("[]")) {
            try {
                tags = JSON.parseArray(entity.getTags(), String.class);
            } catch (Exception e) {
                tags = Collections.emptyList();
            }
        }
        return DocumentVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .category(entity.getCategory())
                .tags(tags)
                .fileType(entity.getFileType())
                .fileUrl(entity.getFileUrl())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
