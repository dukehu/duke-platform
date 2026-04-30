package com.duke.auth.aspect;

import com.duke.auth.entity.SysOperationLog;
import com.duke.auth.mapper.SysOperationLogMapper;
import com.duke.auth.aspect.annotation.OperationLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志异步写库服务。
 * 与 {@link OperationLogAspect} 分离成独立 Bean，是因为
 * Spring AOP 代理机制要求 @Async 方法必须通过 Spring 代理调用，
 * 若直接在切面类中定义 @Async 方法则会退化为同步调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogSaver {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 正则匹配请求参数中的敏感字段（大小写不敏感），将值替换为 ***。
     * 捕获组 $1 保留字段名，只遮盖值，便于排查是否传了该字段。
     */
    private static final java.util.regex.Pattern SENSITIVE_PATTERN =
            java.util.regex.Pattern.compile(
                    "(?i)(\"(?:password|newPassword|oldPassword|token|secret)\")\\s*:\\s*\"[^\"]*\"");

    /**
     * 异步写入操作日志，使用独立线程池 logExecutor，不阻塞业务请求。
     * 所有请求上下文（IP、URL、用户名）由调用方在主线程提前传入，
     * 此处不访问 ThreadLocal 相关的上下文。
     */
    @Async("logExecutor")
    public void save(ProceedingJoinPoint point, OperationLog annotation,
                     Object result, String errorMsg, int status, long costTime,
                     String requestUrl, String requestMethod, String operatorIp, String username) {
        try {
            SysOperationLog logEntity = new SysOperationLog();
            logEntity.setModule(annotation.module());
            logEntity.setOperation(annotation.operation());
            logEntity.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
            logEntity.setCostTime(costTime);
            logEntity.setStatus(status);
            logEntity.setErrorMsg(errorMsg);
            logEntity.setOperatorName(username);
            logEntity.setCreateTime(LocalDateTime.now());
            logEntity.setRequestUrl(requestUrl);
            logEntity.setRequestMethod(requestMethod);
            logEntity.setOperatorIp(operatorIp);

            try {
                String rawParams = objectMapper.writeValueAsString(point.getArgs());
                // 脱敏处理：将密码、token 等字段值替换为 ***
                logEntity.setRequestParams(
                        SENSITIVE_PATTERN.matcher(rawParams).replaceAll("$1:\"***\""));
            } catch (Exception ignored) {}

            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
}
