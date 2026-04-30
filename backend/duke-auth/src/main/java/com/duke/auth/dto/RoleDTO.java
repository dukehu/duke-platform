package com.duke.auth.dto;

import com.duke.framework.dto.PageDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色请求 DTO
 */
@Data
public class RoleDTO extends PageDTO {
    private Long id;
    @NotNull(message = "应用ID不能为空", groups = Create.class)
    private Long appId;
    @NotBlank(message = "角色编码不能为空", groups = Create.class)
    private String roleCode;
    @NotBlank(message = "角色名称不能为空", groups = Create.class)
    private String roleName;
    private String roleDesc;
    private Integer dataScope;
    private Integer status;
    private Integer sortOrder;
    /** 菜单ID列表（分配权限时使用） */
    private List<Long> menuIds;
    /** API ID列表（分配权限时使用） */
    private List<Long> apiIds;
    /** 自定义数据权限部门ID列表 */
    private List<Long> deptIds;

    // 查询条件
    private String keyword;

    public interface Create {}
    public interface Update {}
}
