package com.duke.auth.service.impl;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.LogQueryDTO;
import com.duke.auth.entity.SysOperationLog;
import com.duke.auth.mapper.SysOperationLogMapper;
import com.duke.auth.service.IOperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements IOperationLogService {

    private final SysOperationLogMapper logMapper;

    @Override
    public PageResult<SysOperationLog> page(LogQueryDTO dto) {
        Page<SysOperationLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .like(StringUtils.hasText(dto.getModule()), SysOperationLog::getModule, dto.getModule())
                .like(StringUtils.hasText(dto.getOperatorName()), SysOperationLog::getOperatorName, dto.getOperatorName())
                .eq(dto.getStatus() != null, SysOperationLog::getStatus, dto.getStatus())
                .orderByDesc(SysOperationLog::getCreateTime);
        logMapper.selectPage(page, wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void delete(Long id) {
        logMapper.deleteById(id);
    }
}
