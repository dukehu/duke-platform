package com.duke.auth.service.impl;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.RoleDTO;
import com.duke.auth.entity.*;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.*;
import com.duke.auth.service.IRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleApiMapper roleApiMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleButtonMapper roleButtonMapper;

    @Override
    public PageResult<SysRole> page(RoleDTO dto) {
        Page<SysRole> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(dto.getAppId() != null, SysRole::getAppId, dto.getAppId())
                .like(StringUtils.hasText(dto.getRoleName()), SysRole::getRoleName, dto.getRoleName())
                .eq(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .orderByAsc(SysRole::getSortOrder);
        roleMapper.selectPage(page, wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1).orderByAsc(SysRole::getSortOrder));
    }

    @Override
    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    @Override
    @Transactional
    public void create(RoleDTO dto) {
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, dto.getRoleCode())) > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setAppId(dto.getAppId());
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRoleDesc(dto.getRoleDesc());
        role.setDataScope(dto.getDataScope() != null ? dto.getDataScope() : 1);
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        roleMapper.insert(role);
    }

    @Override
    @Transactional
    public void update(RoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) throw new BusinessException("角色不存在");
        if (StringUtils.hasText(dto.getRoleName())) role.setRoleName(dto.getRoleName());
        if (StringUtils.hasText(dto.getRoleDesc())) role.setRoleDesc(dto.getRoleDesc());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) role.setSortOrder(dto.getSortOrder());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        roleApiMapper.delete(new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, id));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, id));
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds.isEmpty()) return;
        List<SysRoleMenu> list = menuIds.stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            return rm;
        }).collect(Collectors.toList());
        Db.saveBatch(list);
    }

    @Override
    @Transactional
    public void assignApis(Long roleId, List<Long> apiIds) {
        roleApiMapper.delete(new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, roleId));
        if (apiIds.isEmpty()) return;
        List<SysRoleApi> list = apiIds.stream().map(apiId -> {
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            return ra;
        }).collect(Collectors.toList());
        Db.saveBatch(list);
    }

    @Override
    @Transactional
    public void assignDataScope(Long roleId, Integer dataScope, List<Long> deptIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException("角色不存在");
        role.setDataScope(dataScope);
        roleMapper.updateById(role);
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        if (deptIds != null && !deptIds.isEmpty()) {
            List<SysRoleDept> list = deptIds.stream().map(deptId -> {
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(roleId);
                rd.setDeptId(deptId);
                return rd;
            }).collect(Collectors.toList());
            Db.saveBatch(list);
        }
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getApiIds(Long roleId) {
        return roleApiMapper.selectList(
                new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, roleId))
                .stream().map(SysRoleApi::getApiId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignButtons(Long roleId, List<Long> buttonIds) {
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, roleId));
        if (buttonIds.isEmpty()) return;
        List<SysRoleButton> list = buttonIds.stream().map(buttonId -> {
            SysRoleButton rb = new SysRoleButton();
            rb.setRoleId(roleId);
            rb.setButtonId(buttonId);
            return rb;
        }).collect(Collectors.toList());
        Db.saveBatch(list);
    }

    @Override
    public List<Long> getButtonIds(Long roleId) {
        return roleButtonMapper.selectList(
                new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, roleId))
                .stream().map(SysRoleButton::getButtonId).collect(Collectors.toList());
    }
}
