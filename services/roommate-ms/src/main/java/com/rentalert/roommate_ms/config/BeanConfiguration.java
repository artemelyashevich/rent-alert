package com.rentalert.roommate_ms.config;

import com.rentalert.kafka.audit.CurrentUserIdProvider;
import com.rentalert.redis.service.UserSessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@Configuration
public class BeanConfiguration {

    @Bean
    public CurrentUserIdProvider currentUserIdProvider(UserSessionService userSessionService) {
        return () -> userSessionService.getCurrentUser().getUserId();
    }
}
