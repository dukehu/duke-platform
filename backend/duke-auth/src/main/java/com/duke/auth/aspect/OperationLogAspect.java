package com.duke.auth.aspect;

import com.duke.auth.aspect.annotation.OperationLog;
import com.duke.framework.util.IpUtil;
import com.duke.auth.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面，拦截所有标注了 {@link OperationLog} 的方法。
 * 日志写入委托给 {@link OperationLogSaver} 异步执行，不阻塞主请求。
 * 切面本身在主线程运行，因为异步线程中 ThreadLocal 的 RequestContextHolder 已失效，
 * 所以必须在此提前提取 IP、URL、操作人等请求上下文。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogSaver operationLogSaver;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        // 必须在主线程提取，异步线程中 RequestContextHolder 返回 null
        String requestUrl = null;
        String requestMethod = null;
        String operatorIp = null;
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            requestUrl = request.getRequestURI();
            requestMethod = request.getMethod();
            operatorIp = IpUtil.getIpAddr(request);
        }
        String username = SecurityUtil.getCurrentUsername();

        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMsg = null;
        int status = 1;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            status = 0;
            throw e;
        } finally {
            // finally 块保证无论成功还是异常都记录日志
            long costTime = System.currentTimeMillis() - startTime;
            operationLogSaver.save(point, operationLog, result, errorMsg, status, costTime,
                    requestUrl, requestMethod, operatorIp, username);
        }
    }
}
