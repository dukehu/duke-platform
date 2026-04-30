package com.duke.auth.service.impl;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.AppDTO;
import com.duke.auth.entity.SysApp;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.SysAppMapper;
import com.duke.auth.service.IAppService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppServiceImpl implements IAppService {

    private final SysAppMapper appMapper;

    @Override
    public PageResult<SysApp> page(AppDTO dto) {
        Page<SysApp> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysApp> wrapper = new LambdaQueryWrapper<SysApp>()
                .like(StringUtils.hasText(dto.getKeyword()), SysApp::getAppName, dto.getKeyword())
                .eq(dto.getStatus() != null, SysApp::getStatus, dto.getStatus())
                .orderByAsc(SysApp::getSortOrder);
        appMapper.selectPage(page, wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysApp> listAll() {
        return appMapper.selectList(new LambdaQueryWrapper<SysApp>()
                .eq(SysApp::getStatus, 1).orderByAsc(SysApp::getSortOrder));
    }

    @Override
    public void create(AppDTO dto) {
        if (appMapper.selectCount(new LambdaQueryWrapper<SysApp>()
                .eq(SysApp::getAppCode, dto.getAppCode())) > 0) {
            throw new BusinessException("应用编码已存在");
        }
        SysApp app = new SysApp();
        app.setAppCode(dto.getAppCode());
        app.setAppName(dto.getAppName());
        app.setAppDesc(dto.getAppDesc());
        app.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        app.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        appMapper.insert(app);
    }

    @Override
    public void update(AppDTO dto) {
        SysApp app = appMapper.selectById(dto.getId());
        if (app == null) throw new BusinessException("应用不存在");
        if (StringUtils.hasText(dto.getAppName())) app.setAppName(dto.getAppName());
        if (StringUtils.hasText(dto.getAppDesc())) app.setAppDesc(dto.getAppDesc());
        if (dto.getStatus() != null) app.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) app.setSortOrder(dto.getSortOrder());
        appMapper.updateById(app);
    }

    @Override
    public void delete(Long id) {
        appMapper.deleteById(id);
    }
}
