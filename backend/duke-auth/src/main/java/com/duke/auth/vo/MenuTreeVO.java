package com.duke.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private List<ButtonVO> buttons;
    private List<MenuTreeVO> children;
}

