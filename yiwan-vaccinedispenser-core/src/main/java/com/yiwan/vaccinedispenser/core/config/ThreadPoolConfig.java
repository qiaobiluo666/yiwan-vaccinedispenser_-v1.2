package com.yiwan.vaccinedispenser.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author slh
 * @date 2023/10/25
 * @Description
 */
@Configuration
public class ThreadPoolConfig {
    @Bean("DispensingThreadPool")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7); // 核心线程数
        executor.setMaxPoolSize(10); // 最大线程数
        executor.setQueueCapacity(200); // 线程池队列容量
        executor.setThreadNamePrefix("Dispensing-"); // 线程名称前缀
        executor.initialize();
        return executor;
    }
}
