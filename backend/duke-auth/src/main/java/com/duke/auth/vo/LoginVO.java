package com.duke.auth.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class LoginVO {
    private String token;
    private String username;
    private String realName;
    private String avatar;
    private Set<String> buttons;
    private List<String> roles;
}
