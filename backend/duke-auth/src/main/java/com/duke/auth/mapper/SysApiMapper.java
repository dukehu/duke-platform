package com.duke.auth.mapper;

import com.duke.auth.entity.SysApi;
import com.duke.auth.vo.ApiRuleVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysApiMapper extends BaseMapper<SysApi> {
    @Select("""
            SELECT DISTINCT a.permission FROM sys_api a
            INNER JOIN sys_role_api ra ON a.id = ra.api_id
            INNER JOIN sys_user_role ur ON ra.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND a.permission IS NOT NULL AND a.permission != ''
              AND a.status = 1 AND a.deleted = 0
            """)
    Set<String> selectApiPermissionsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT permission FROM sys_api WHERE permission IS NOT NULL AND permission != '' AND status = 1 AND
                deleted = 0
            """)
    Set<String> selectAllApiPermissions();

    @Select("""
            SELECT api_path, api_method, permission, app_id FROM sys_api
            WHERE status = 1 AND deleted = 0
              AND (#{appId} IS NULL OR app_id = #{appId})
            """)
    List<ApiRuleVO> selectApiRulesByAppId(@Param("appId") String appId);
}
