package com.duke.auth.service;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.RoleDTO;
import com.duke.auth.entity.SysRole;

import java.util.List;

public interface IRoleService {
    PageResult<SysRole> page(RoleDTO dto);
    List<SysRole> list();
    SysRole getById(Long id);
    void create(RoleDTO dto);
    void update(RoleDTO dto);
    void delete(Long id);
    void assignMenus(Long roleId, List<Long> menuIds);
    void assignApis(Long roleId, List<Long> apiIds);
    void assignDataScope(Long roleId, Integer dataScope, List<Long> deptIds);
    List<Long> getMenuIds(Long roleId);
    List<Long> getApiIds(Long roleId);
    void assignButtons(Long roleId, List<Long> buttonIds);
    List<Long> getButtonIds(Long roleId);
}
