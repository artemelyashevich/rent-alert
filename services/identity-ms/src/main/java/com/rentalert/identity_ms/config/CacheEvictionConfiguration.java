package com.rentalert.identity_ms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CacheEvictionConfiguration {

    @Bean
    public ThreadPoolTaskScheduler cacheEvictionScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("cache-evict-");
        return scheduler;
    }
}
