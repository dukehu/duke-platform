package com.duke.auth.security;

import com.duke.auth.entity.SysRole;
import com.duke.auth.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {

    private final SysUser user;
    /** 权限标识符集合（button_code + api permission，用于 @PreAuthorize） */
    private final Set<String> permissions;
    /** 按钮编码集合（仅 button_code，返回前端用于 v-permission） */
    private final Set<String> buttonCodes;
    /** 角色编码列表 */
    private final List<String> roles;
    /** 角色实体列表（含 dataScope，供数据权限切面使用，避免重复查库） */
    private final List<SysRole> roleEntities;
    /** 所属部门ID列表 */
    private final List<Long> deptIds;
    /** 主部门ID */
    private final Long primaryDeptId;

    public LoginUser(SysUser user, Set<String> permissions, Set<String> buttonCodes,
                     List<String> roles, List<SysRole> roleEntities,
                     List<Long> deptIds, Long primaryDeptId) {
        this.user = user;
        this.permissions = permissions;
        this.buttonCodes = buttonCodes;
        this.roles = roles;
        this.roleEntities = roleEntities;
        this.deptIds = deptIds;
        this.primaryDeptId = primaryDeptId;
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
