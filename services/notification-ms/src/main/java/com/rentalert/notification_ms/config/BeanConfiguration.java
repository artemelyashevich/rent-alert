package com.rentalert.notification_ms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }

}
