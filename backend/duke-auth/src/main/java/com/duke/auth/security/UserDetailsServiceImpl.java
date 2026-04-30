package com.duke.auth.security;

import com.duke.framework.common.Constants;
import com.duke.framework.common.ResultCode;
import com.duke.auth.entity.SysRole;
import com.duke.auth.entity.SysUser;
import com.duke.auth.entity.SysUserDept;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duke.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 用户详情加载服务。
 * 登录时 AuthenticationManager 调用此处，后续 JWT 过滤器也会在每次请求时调用。
 * 为降低每次请求的查库次数，所有关联数据（角色、权限、部门）在此一次性加载，
 * 缓存在 LoginUser 对象中，切面直接从 SecurityContext 读取，无需二次查库。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysButtonMapper buttonMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysApiMapper apiMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException(ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 角色查询只执行一次：同时用于构建角色列表和数据权限切面（DataScopeAspect 直接从 LoginUser 取）
        List<SysRole> roleEntities = roleMapper.selectRolesByUserId(user.getId());
        List<String> roles = roleEntities.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // 超级管理员加载全量权限，普通用户只加载自身角色授权的权限
        boolean superAdmin = isSuperAdmin(user.getId());
        Set<String> buttonCodes = superAdmin
                ? buttonMapper.selectAllButtonCodes()
                : buttonMapper.selectButtonCodesByUserId(user.getId());
        Set<String> apiPermissions = superAdmin
                ? apiMapper.selectAllApiPermissions()
                : apiMapper.selectApiPermissionsByUserId(user.getId());
        // authorities = buttonCodes ∪ apiPermissions，供 @PreAuthorize 校验
        Set<String> permissions = new java.util.HashSet<>(buttonCodes);
        permissions.addAll(apiPermissions);

        // 加载部门信息（支持一用户多部门，isPrimary=1 为主部门）
        List<SysUserDept> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, user.getId()));
        List<Long> deptIds = userDepts.stream().map(SysUserDept::getDeptId).collect(Collectors.toList());
        Long primaryDeptId = userDepts.stream()
                .filter(d -> d.getIsPrimary() == 1)
                .map(SysUserDept::getDeptId)
                .findFirst().orElse(null);

        return new LoginUser(user, permissions, buttonCodes, roles, roleEntities, deptIds, primaryDeptId);
    }

    private boolean isSuperAdmin(Long userId) {
        return roleMapper.countSuperAdminByUserId(userId, Constants.SUPER_ADMIN_ROLE) > 0;
    }
}
