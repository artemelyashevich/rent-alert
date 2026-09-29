package com.rentalert.internal.configuration;

import com.rentalert.internal.client.IdentityMsClient;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.IdentityMsServiceImpl;
import com.rentalert.internal.decoder.InternalErrorDecoder;
import com.rentalert.internal.interceptor.HeaderPropagationInterceptor;
import com.rentalert.internal.interceptor.InternalTokenInterceptor;
import com.rentalert.redis.service.CacheService;
import feign.Logger;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
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

    @Bean
    @ConditionalOnMissingBean
    public InternalTokenInterceptor internalTokenInterceptor(@Value("${app.internal.token:}") String token) {
        return new InternalTokenInterceptor(token);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityMsService identityMsService(@Lazy IdentityMsClient identityMsClient, @Lazy CacheService cacheService,
                                               RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new IdentityMsServiceImpl(identityMsClient, cacheService, retryRegistry, circuitBreakerRegistry);
    }
}
