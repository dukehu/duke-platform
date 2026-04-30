package com.duke.auth.enums;

import lombok.Getter;

/**
 * 菜单类型枚举
 */
@Getter
public enum MenuTypeEnum {

    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final Integer code;
    private final String desc;

    MenuTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
