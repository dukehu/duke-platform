package com.duke.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 VO
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private List<String> roleNames;
    private List<Long> roleIds;
    private List<Long> deptIds;
    private String primaryDeptName;
}
