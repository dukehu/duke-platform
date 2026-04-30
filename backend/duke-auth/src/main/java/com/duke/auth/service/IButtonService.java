package com.duke.auth.service;

import com.duke.auth.dto.ButtonDTO;
import com.duke.auth.entity.SysButton;

import java.util.List;

public interface IButtonService {
    List<SysButton> list(Long menuId);
    void create(ButtonDTO dto);
    void update(ButtonDTO dto);
    void delete(Long id);
}
