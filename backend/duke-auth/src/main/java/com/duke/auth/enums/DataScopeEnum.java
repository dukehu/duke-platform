package com.duke.auth.enums;

import lombok.Getter;

/**
 * 数据权限范围枚举
 */
@Getter
public enum DataScopeEnum {

    ALL(1, "全部数据"),
    CUSTOM(2, "自定义部门"),
    DEPT(3, "本部门"),
    DEPT_AND_CHILD(4, "本部门及下级"),
    SELF(5, "仅本人");

    private final Integer code;
    private final String desc;

    DataScopeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
