package com.apiprecios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** Limit browser use; do not let an LLM launch unlimited concurrent jobs. */
@Configuration
@EnableAsync
public class AgentAsyncConfig {

    @Bean("priceRefreshExecutor")
    public Executor priceRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("price-refresh-");
        executor.initialize();
        return executor;
    }
}
