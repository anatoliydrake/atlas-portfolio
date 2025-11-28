package com.atlas.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "priceRefreshExecutor", destroyMethod = "shutdown")
    public Executor priceRefreshExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
