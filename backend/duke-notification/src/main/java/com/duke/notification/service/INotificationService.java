package com.duke.notification.service;

import com.duke.notification.dto.NotificationQueryDTO;
import com.duke.framework.common.PageResult;

public interface INotificationService {

    PageResult<Object> page(NotificationQueryDTO dto);

    Object getById(Long id);

    void delete(Long id);
}



