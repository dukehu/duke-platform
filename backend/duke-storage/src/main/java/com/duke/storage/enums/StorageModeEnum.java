package com.duke.storage.enums;

import lombok.Getter;

/**
 * 存储模式枚举
 */
@Getter
public enum StorageModeEnum {

    LOCAL("local", "本地存储"),
    MINIO("minio", "MinIO对象存储");

    private final String code;
    private final String desc;

    StorageModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static StorageModeEnum fromCode(String code) {
        for (StorageModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知的存储模式: " + code);
    }
}
