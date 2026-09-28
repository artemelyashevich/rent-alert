package com.rentalert.internal.configuration;

import feign.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@EnableFeignClients("com.rentalert")
public class CommonFeignConfiguration {

    @Bean
    @ConditionalOnProperty(value = "client.server.log-mode", havingValue = "true")
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
