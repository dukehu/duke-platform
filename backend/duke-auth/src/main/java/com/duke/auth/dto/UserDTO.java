package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 用户请求 DTO
 */
@Data
public class UserDTO extends PageDTO {
    private Long id;
    @NotBlank(message = "用户名不能为空", groups = Create.class)
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    /** 角色ID列表 */
    private List<Long> roleIds;
    /** 部门ID列表 */
    private List<Long> deptIds;
    /** 主部门ID */
    private Long primaryDeptId;

    // 查询条件
    private String keyword;
    private Long deptId;

    public interface Create {}
    public interface Update {}
}
