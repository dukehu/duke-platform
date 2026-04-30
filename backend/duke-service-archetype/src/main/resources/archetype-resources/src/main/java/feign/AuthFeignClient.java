package ${package}.feign;

import com.duke.framework.common.Result;
import com.duke.framework.feign.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 璋冪敤 duke-auth 鐨?FeignClient 绀轰緥
 * 浣跨敤 InternalFeignConfig 鑷姩娉ㄥ叆 X-Gateway-Secret 璇锋眰澶? * 褰?useFeign=n 鏃跺彲鍒犻櫎姝ゆ枃浠跺強 feign 鍖? */
#if($useFeign != "y")
// 娉ㄦ剰锛歶seFeign=n锛屾鏂囦欢浠呬綔鍗犱綅锛屽彲瀹夊叏鍒犻櫎
#end
@FeignClient(
        name = "duke-auth",
        contextId = "authFeignClient",
        configuration = InternalFeignConfig.class
)
public interface AuthFeignClient {

    @GetMapping("/internal/users/{userId}")
    Result<Object> getUserById(@PathVariable Long userId);
}



