package com.duke.auth.aspect;

import com.duke.auth.aspect.annotation.DataScope;
import com.duke.auth.entity.SysRole;
import com.duke.auth.enums.DataScopeEnum;
import com.duke.auth.security.LoginUser;
import com.duke.auth.util.SecurityUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据权限切面，在标注了 {@link DataScope} 的方法执行前，
 * 向第一个参数的 params 字段注入 SQL WHERE 条件片段，
 * MyBatis XML 中通过 ${params.dataScope} 引用。
 *
 * <p>数据范围等级（数字越小权限越大）：
 * <ul>
 *   <li>1 - 全部数据</li>
 *   <li>2 - 自定义部门（sys_role_dept 配置）</li>
 *   <li>3 - 本部门</li>
 *   <li>4 - 本部门及下级（利用 ancestors 字段的 FIND_IN_SET 查询）</li>
 *   <li>5 - 仅本人</li>
 * </ul>
 */
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void before(JoinPoint point, DataScope dataScope) {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null) return;

        // 超级管理员不限制数据范围
        if (loginUser.getPermissions().contains("*:*:*")) return;

        // 角色实体已在登录时加载到 LoginUser，直接取，无需再查库
        List<SysRole> roles = loginUser.getRoleEntities();
        if (roles.isEmpty()) return;

        // 用户可能拥有多个角色，取最宽松的数据权限（scope 值最小 = 权限最大）
        int minScope = roles.stream().mapToInt(SysRole::getDataScope).min().orElse(DataScopeEnum.ALL.getCode());

        String sqlCondition = buildSqlCondition(minScope, dataScope, loginUser, roles);
        if (sqlCondition == null) return;

        // 将 SQL 条件注入到方法第一个参数的 params 字段（QueryDTO 约定字段）
        Object[] args = point.getArgs();
        if (args.length > 0 && args[0] != null) {
            injectDataScope(args[0], sqlCondition);
        }
    }

    private String buildSqlCondition(int scope, DataScope dataScope, LoginUser loginUser, List<SysRole> roles) {
        String deptAlias = dataScope.deptAlias();
        String userAlias = dataScope.userAlias();

        return switch (scope) {
            case 1 -> null; // 全部数据，不添加过滤条件
            case 2 -> {     // 自定义部门：查角色关联的部门
                List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
                String roleIdStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                yield deptAlias + ".id IN (SELECT dept_id FROM sys_role_dept WHERE role_id IN (" + roleIdStr + "))";
            }
            case 3 -> {     // 本部门：用户直属部门
                if (loginUser.getDeptIds().isEmpty()) yield "1=0";
                String deptIdStr = loginUser.getDeptIds().stream().map(String::valueOf).collect(Collectors.joining(","));
                yield deptAlias + ".id IN (" + deptIdStr + ")";
            }
            case 4 -> {     // 本部门及下级：利用 ancestors 字段做树形查询
                if (loginUser.getDeptIds().isEmpty()) yield "1=0";
                String deptIdStr = loginUser.getDeptIds().stream().map(String::valueOf).collect(Collectors.joining(","));
                yield deptAlias + ".id IN (SELECT id FROM sys_dept WHERE id IN (" + deptIdStr
                        + ") OR FIND_IN_SET(id, (SELECT GROUP_CONCAT(ancestors) FROM sys_dept WHERE id IN (" + deptIdStr + "))))";
            }
            case 5 -> userAlias + ".id = " + loginUser.getUserId(); // 仅本人
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private void injectDataScope(Object arg, String sqlCondition) {
        try {
            var paramsField = arg.getClass().getDeclaredField("params");
            paramsField.setAccessible(true);
            Object params = paramsField.get(arg);
            if (params instanceof Map map) {
                map.put("dataScope", sqlCondition);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // DTO 没有 params 字段时静默忽略，不影响查询结果
        }
    }
}
