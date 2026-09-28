package com.rentalert.internal.configuration;

import com.rentalert.internal.decoder.InternalErrorDecoder;
import com.rentalert.internal.interceptor.HeaderPropagationInterceptor;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableFeignClients("com.rentalert")
@PropertySource("classpath:internal-integration-defaults.properties")
public class CommonFeignConfiguration {

    @Bean
    @ConditionalOnProperty(value = "client.server.log-mode", havingValue = "true")
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder internalErrorDecoder() {
        return new InternalErrorDecoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public HeaderPropagationInterceptor headerPropagationInterceptor() {
        return new HeaderPropagationInterceptor();
    }
}
