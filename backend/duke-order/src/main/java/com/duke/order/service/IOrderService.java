package com.duke.order.service;

import com.duke.order.dto.OrderQueryDTO;
import com.duke.framework.common.PageResult;

public interface IOrderService {

    PageResult<Object> page(OrderQueryDTO dto);

    Object getById(Long id);

    void delete(Long id);
}



