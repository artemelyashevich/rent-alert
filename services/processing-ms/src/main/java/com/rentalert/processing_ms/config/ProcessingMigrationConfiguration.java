
package com.rentalert.processing_ms.config;

import com.rentalert.common.service.LockService;
import com.rentalert.kafka.migration.MigrationControl;
import com.rentalert.kafka.migration.RedisMigrationControl;
import com.rentalert.redis.service.CacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessingMigrationConfiguration {

    @Bean
    public MigrationControl migrationControl(CacheService cacheService, LockService lockService) {
        return new RedisMigrationControl(cacheService, lockService);
    }
}
