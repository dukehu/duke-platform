package com.duke.knowledgeqa.dto;

import com.duke.framework.dto.PageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class DocumentQueryDTO extends PageDTO {

    private String category;

    private String keyword;

    private String status;
}
