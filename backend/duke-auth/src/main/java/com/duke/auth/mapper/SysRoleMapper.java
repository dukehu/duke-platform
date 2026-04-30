package com.duke.auth.mapper;

import com.duke.auth.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
        SELECT COUNT(*) FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId} AND r.role_code = #{roleCode} AND r.deleted = 0
        """)
    int countSuperAdminByUserId(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Select("""
        SELECT r.* FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
        """)
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
