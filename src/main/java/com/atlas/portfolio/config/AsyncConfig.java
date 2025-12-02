package com.atlas.portfolio.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableScheduling
@EnableCaching
public class AsyncConfig {

    private static final int PRICE_REFRESH_THREAD_POOL_SIZE = 10;

    @Bean(name = "priceRefreshExecutor", destroyMethod = "shutdown")
    public Executor priceRefreshExecutor() {
        return Executors.newFixedThreadPool(PRICE_REFRESH_THREAD_POOL_SIZE);
    }
}
