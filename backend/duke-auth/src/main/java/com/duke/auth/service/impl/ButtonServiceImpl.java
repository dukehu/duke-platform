package com.duke.auth.service.impl;

import com.duke.auth.dto.ButtonDTO;
import com.duke.auth.entity.SysButton;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.SysButtonMapper;
import com.duke.auth.service.IButtonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ButtonServiceImpl implements IButtonService {

    private final SysButtonMapper buttonMapper;

    @Override
    public List<SysButton> list(Long menuId) {
        return buttonMapper.selectByMenuId(menuId);
    }

    @Override
    public void create(ButtonDTO dto) {
        SysButton button = new SysButton();
        button.setMenuId(dto.getMenuId());
        button.setButtonName(dto.getButtonName());
        button.setButtonCode(dto.getButtonCode());
        button.setButtonType(dto.getButtonType());
        button.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        button.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        buttonMapper.insert(button);
    }

    @Override
    public void update(ButtonDTO dto) {
        SysButton button = buttonMapper.selectById(dto.getId());
        if (button == null) throw new BusinessException("按钮不存在");
        if (dto.getButtonName() != null) button.setButtonName(dto.getButtonName());
        if (dto.getButtonCode() != null) button.setButtonCode(dto.getButtonCode());
        if (dto.getButtonType() != null) button.setButtonType(dto.getButtonType());
        if (dto.getSortOrder() != null) button.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) button.setStatus(dto.getStatus());
        buttonMapper.updateById(button);
    }

    @Override
    public void delete(Long id) {
        buttonMapper.deleteById(id);
    }
}
