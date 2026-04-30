package com.duke.knowledgeqa.feign;

import com.duke.framework.common.Result;
import com.duke.framework.feign.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "duke-auth",
    contextId = "authFeignClient",
    configuration = InternalFeignConfig.class
)
public interface AuthFeignClient {

    @GetMapping("/internal/users/{userId}")
    Result<Object> getUserById(@PathVariable Long userId);
}
