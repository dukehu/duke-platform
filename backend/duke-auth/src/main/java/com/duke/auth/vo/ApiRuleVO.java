package com.duke.auth.vo;

import lombok.Data;

@Data
public class ApiRuleVO {
    private String apiPath;
    private String apiMethod;
    private String permission;
    private String appId;
}
