package com.duke.auth.service.impl;

import com.duke.auth.dto.MenuDTO;
import com.duke.auth.entity.SysButton;
import com.duke.auth.entity.SysMenu;
import com.duke.auth.entity.SysRoleMenu;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.SysButtonMapper;
import com.duke.auth.mapper.SysMenuMapper;
import com.duke.auth.mapper.SysRoleMenuMapper;
import com.duke.auth.service.IMenuService;
import com.duke.auth.vo.ButtonVO;
import com.duke.auth.vo.MenuTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysButtonMapper buttonMapper;

    @Override
    public List<MenuTreeVO> tree(Long appId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(appId != null, SysMenu::getAppId, appId)
                .orderByAsc(SysMenu::getSortOrder);
        List<SysMenu> all = menuMapper.selectList(wrapper);

        List<Long> menuIds = all.stream()
                .filter(m -> Integer.valueOf(2).equals(m.getMenuType()))
                .map(SysMenu::getId)
                .collect(Collectors.toList());

        Map<Long, List<ButtonVO>> buttonMap = Collections.emptyMap();
        if (!menuIds.isEmpty()) {
            List<SysButton> buttons = buttonMapper.selectList(
                    new LambdaQueryWrapper<SysButton>()
                            .in(SysButton::getMenuId, menuIds)
                            .eq(SysButton::getStatus, 1)
                            .orderByAsc(SysButton::getSortOrder));
            buttonMap = buttons.stream().collect(Collectors.groupingBy(
                    SysButton::getMenuId,
                    Collectors.mapping(b -> {
                        ButtonVO vo = new ButtonVO();
                        vo.setId(b.getId());
                        vo.setButtonName(b.getButtonName());
                        vo.setButtonCode(b.getButtonCode());
                        vo.setButtonType(b.getButtonType());
                        vo.setSortOrder(b.getSortOrder());
                        return vo;
                    }, Collectors.toList())));
        }

        return buildTree(all, 0L, buttonMap);
    }

    @Override
    public SysMenu getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new BusinessException("菜单不存在");
        return menu;
    }

    @Override
    public void create(MenuDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        menu.setAppId(dto.getAppId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : 1);
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        menuMapper.insert(menu);
    }

    @Override
    public void update(MenuDTO dto) {
        SysMenu menu = menuMapper.selectById(dto.getId());
        if (menu == null) throw new BusinessException("菜单不存在");
        if (StringUtils.hasText(dto.getMenuName())) menu.setMenuName(dto.getMenuName());
        if (StringUtils.hasText(dto.getPath())) menu.setPath(dto.getPath());
        if (StringUtils.hasText(dto.getComponent())) menu.setComponent(dto.getComponent());
        if (StringUtils.hasText(dto.getIcon())) menu.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) menu.setSortOrder(dto.getSortOrder());
        if (dto.getVisible() != null) menu.setVisible(dto.getVisible());
        if (dto.getStatus() != null) menu.setStatus(dto.getStatus());
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw new BusinessException("存在子菜单，不允许删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> all, Long parentId, Map<Long, List<ButtonVO>> buttonMap) {
        return all.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    MenuTreeVO vo = new MenuTreeVO();
                    vo.setId(m.getId());
                    vo.setParentId(m.getParentId());
                    vo.setMenuName(m.getMenuName());
                    vo.setMenuType(m.getMenuType());
                    vo.setPath(m.getPath());
                    vo.setComponent(m.getComponent());
                    vo.setIcon(m.getIcon());
                    vo.setSortOrder(m.getSortOrder());
                    vo.setVisible(m.getVisible());
                    if (Integer.valueOf(2).equals(m.getMenuType())) {
                        vo.setButtons(buttonMap.getOrDefault(m.getId(), Collections.emptyList()));
                    }
                    vo.setChildren(buildTree(all, m.getId(), buttonMap));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
