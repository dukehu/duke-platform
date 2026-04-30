package com.duke.auth.service;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.ApiQueryDTO;
import com.duke.auth.entity.SysApi;
import java.util.List;
import java.util.Map;

public interface IApiService {
    PageResult<SysApi> page(ApiQueryDTO dto);
    Map<String, List<Map<String, String>>> listControllers();
    Map<String, Map<String, List<SysApi>>> listGrouped();
    void sync();
    void syncAppApis(String appId);
    void updateStatus(Long id, Integer status);
    void updatePermission(Long id, String permission);
}
