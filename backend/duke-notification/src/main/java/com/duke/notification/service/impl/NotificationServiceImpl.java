package com.duke.notification.service.impl;

import com.duke.notification.dto.NotificationQueryDTO;
import com.duke.notification.service.INotificationService;
import com.duke.framework.common.PageResult;
import com.duke.framework.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {


    @Override
    public PageResult<Object> page(NotificationQueryDTO dto) {
        // TODO 实现分页查询
        return PageResult.of(0L, List.of());
    }

    @Override
    public Object getById(Long id) {
        // TODO 实现详情查询
        throw new BusinessException("功能待实现");
    }

    @Override
    public void delete(Long id) {
        // TODO 实现删除逻辑
        throw new BusinessException("功能待实现");
    }
}
