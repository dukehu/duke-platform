package com.duke.auth.service;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.LogQueryDTO;
import com.duke.auth.entity.SysOperationLog;

public interface IOperationLogService {
    PageResult<SysOperationLog> page(LogQueryDTO dto);
    void delete(Long id);
}
