package com.duke.auth.mapper;

import com.duke.auth.entity.SysButton;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysButtonMapper extends BaseMapper<SysButton> {

    @Select("""
        SELECT DISTINCT b.button_code FROM sys_button b
        INNER JOIN sys_role_button rb ON b.id = rb.button_id
        INNER JOIN sys_user_role ur ON rb.role_id = ur.role_id
        WHERE ur.user_id = #{userId} AND b.status = 1 AND b.deleted = 0
        """)
    Set<String> selectButtonCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT button_code FROM sys_button WHERE status = 1 AND deleted = 0")
    Set<String> selectAllButtonCodes();

    @Select("SELECT * FROM sys_button WHERE menu_id = #{menuId} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SysButton> selectByMenuId(@Param("menuId") Long menuId);
}

