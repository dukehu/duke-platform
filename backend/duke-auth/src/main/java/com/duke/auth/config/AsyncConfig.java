package com.duke.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 * 操作日志写库属于非关键路径，使用独立线程池隔离，
 * 避免日志写入慢影响主请求响应时间。
 */
@Configuration
public class AsyncConfig {

    /**
     * 操作日志专用线程池。
     * 使用 DiscardPolicy：队列满时直接丢弃日志任务，
     * 宁可少记录几条日志，也不阻塞业务线程或抛出 RejectedExecutionException。
     */
    @Bean("logExecutor")
    public ThreadPoolTaskExecutor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("log-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
