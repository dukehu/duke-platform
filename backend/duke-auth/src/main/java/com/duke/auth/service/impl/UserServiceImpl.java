package com.duke.auth.service.impl;

import com.duke.framework.common.Constants;
import com.duke.framework.common.PageResult;
import com.duke.framework.common.ResultCode;
import com.duke.auth.dto.UserDTO;
import com.duke.auth.entity.*;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.*;
import com.duke.auth.service.IUserService;
import com.duke.auth.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    /** 至少 8 位，包含字母和数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> page(UserDTO dto) {
        Page<SysUser> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(dto.getKeyword()), w -> w
                        .like(SysUser::getUsername, dto.getKeyword())
                        .or().like(SysUser::getRealName, dto.getKeyword()))
                .eq(dto.getStatus() != null, SysUser::getStatus, dto.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        if (dto.getDeptId() != null) {
            List<Long> deptUserIds = userDeptMapper.selectList(
                    new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getDeptId, dto.getDeptId()))
                    .stream().map(SysUserDept::getUserId).collect(Collectors.toList());
            if (deptUserIds.isEmpty()) return PageResult.of(0L, List.of());
            wrapper.in(SysUser::getId, deptUserIds);
        }
        userMapper.selectPage(page, wrapper);
        List<SysUser> users = page.getRecords();
        if (users.isEmpty()) return PageResult.of(page.getTotal(), List.of());

        // 批量预加载角色和部门，消除 N+1
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());

        // 一次查所有用户角色映射 → 按 userId 分组
        List<SysUserRole> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        Set<Long> roleIds = allUserRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Map.of() :
                roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        Map<Long, List<SysRole>> userRoleMap = allUserRoles.stream()
                .filter(ur -> roleMap.containsKey(ur.getRoleId()))
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(ur -> roleMap.get(ur.getRoleId()), Collectors.toList())));

        // 一次查所有用户部门映射 → 主部门名称批量查
        List<SysUserDept> allUserDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().in(SysUserDept::getUserId, userIds));
        Set<Long> primaryDeptIds = allUserDepts.stream()
                .filter(ud -> ud.getIsPrimary() != null && ud.getIsPrimary() == 1)
                .map(SysUserDept::getDeptId).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = primaryDeptIds.isEmpty() ? Map.of() :
                deptMapper.selectBatchIds(primaryDeptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Map<Long, List<SysUserDept>> userDeptMap = allUserDepts.stream()
                .collect(Collectors.groupingBy(SysUserDept::getUserId));

        List<UserVO> records = users.stream()
                .map(u -> toVO(u, userRoleMap, userDeptMap, deptNameMap))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), records);
    }

    @Override
    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return toSingleVO(user);
    }

    @Override
    @Transactional
    public void create(UserDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        String rawPassword = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "Aa123456";
        validatePasswordStrength(rawPassword);
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userMapper.insert(user);
        if (dto.getRoleIds() != null) saveUserRoles(user.getId(), dto.getRoleIds());
        if (dto.getDeptIds() != null) saveUserDepts(user.getId(), dto.getDeptIds(), dto.getPrimaryDeptId());
    }

    @Override
    @Transactional
    public void update(UserDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (StringUtils.hasText(dto.getRealName())) user.setRealName(dto.getRealName());
        if (StringUtils.hasText(dto.getEmail())) user.setEmail(dto.getEmail());
        if (StringUtils.hasText(dto.getPhone())) user.setPhone(dto.getPhone());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        if (dto.getDeptIds() != null) {
            userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, dto.getId()));
            saveUserDepts(dto.getId(), dto.getDeptIds(), dto.getPrimaryDeptId());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (roleMapper.countSuperAdminByUserId(id, Constants.SUPER_ADMIN_ROLE) > 0) {
            throw new BusinessException("超级管理员账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, id));
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码至少 8 位，且需包含字母和数字");
        }
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        saveUserRoles(userId, roleIds);
    }

    @Override
    @Transactional
    public void assignDepts(Long userId, List<Long> deptIds, Long primaryDeptId) {
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        saveUserDepts(userId, deptIds, primaryDeptId);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds.isEmpty()) return;
        List<SysUserRole> list = roleIds.stream().map(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).collect(Collectors.toList());
        Db.saveBatch(list);
    }

    private void saveUserDepts(Long userId, List<Long> deptIds, Long primaryDeptId) {
        if (deptIds.isEmpty()) return;
        List<SysUserDept> list = deptIds.stream().map(deptId -> {
            SysUserDept ud = new SysUserDept();
            ud.setUserId(userId);
            ud.setDeptId(deptId);
            ud.setIsPrimary(deptId.equals(primaryDeptId) ? 1 : 0);
            return ud;
        }).collect(Collectors.toList());
        Db.saveBatch(list);
    }

    /** 分页用：利用预加载 Map 组装，零额外 SQL */
    private UserVO toVO(SysUser user, Map<Long, List<SysRole>> userRoleMap,
                        Map<Long, List<SysUserDept>> userDeptMap, Map<Long, String> deptNameMap) {
        UserVO vo = buildBaseVO(user);
        List<SysRole> roles = userRoleMap.getOrDefault(user.getId(), List.of());
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        List<SysUserDept> depts = userDeptMap.getOrDefault(user.getId(), List.of());
        vo.setDeptIds(depts.stream().map(SysUserDept::getDeptId).collect(Collectors.toList()));
        depts.stream().filter(ud -> ud.getIsPrimary() != null && ud.getIsPrimary() == 1)
                .findFirst().ifPresent(ud -> vo.setPrimaryDeptName(deptNameMap.get(ud.getDeptId())));
        return vo;
    }

    /** 单用户详情用：按需查询 */
    private UserVO toSingleVO(SysUser user) {
        UserVO vo = buildBaseVO(user);
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        List<SysUserDept> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, user.getId()));
        vo.setDeptIds(userDepts.stream().map(SysUserDept::getDeptId).collect(Collectors.toList()));
        userDepts.stream().filter(ud -> ud.getIsPrimary() != null && ud.getIsPrimary() == 1)
                .findFirst().ifPresent(ud -> {
                    SysDept dept = deptMapper.selectById(ud.getDeptId());
                    if (dept != null) vo.setPrimaryDeptName(dept.getDeptName());
                });
        return vo;
    }

    private UserVO buildBaseVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
