package com.duke.auth.vo;

import lombok.Data;

@Data
public class ButtonVO {
    private Long id;
    private String buttonName;
    private String buttonCode;
    private Integer buttonType;
    private Integer sortOrder;
}
