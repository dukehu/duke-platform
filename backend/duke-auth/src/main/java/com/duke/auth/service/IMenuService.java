package com.duke.auth.service;

import com.duke.auth.dto.MenuDTO;
import com.duke.auth.entity.SysMenu;
import com.duke.auth.vo.MenuTreeVO;

import java.util.List;

public interface IMenuService {
    List<MenuTreeVO> tree(Long appId);
    SysMenu getById(Long id);
    void create(MenuDTO dto);
    void update(MenuDTO dto);
    void delete(Long id);
}
