package com.duke.auth.service;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.AppDTO;
import com.duke.auth.entity.SysApp;

import java.util.List;

public interface IAppService {
    PageResult<SysApp> page(AppDTO dto);
    List<SysApp> listAll();
    void create(AppDTO dto);
    void update(AppDTO dto);
    void delete(Long id);
}
