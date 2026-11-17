package com.rentalert.realtime_ms.config;

import com.rentalert.realtime_ms.service.RecentEventCache;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    private static final int DEDUPLICATION_MAX_SIZE = 10_000;
    private static final Duration DEDUPLICATION_TTL = Duration.ofMinutes(10);

    @Bean
    public RecentEventCache recentEventCache() {
        return new RecentEventCache(DEDUPLICATION_MAX_SIZE, DEDUPLICATION_TTL, Clock.systemUTC());
    }
}
