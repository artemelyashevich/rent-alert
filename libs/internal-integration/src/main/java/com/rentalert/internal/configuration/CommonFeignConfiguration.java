package com.rentalert.internal.configuration;

import com.rentalert.internal.client.IdentityMsClient;
import com.rentalert.internal.client.ListingMsClient;
import com.rentalert.internal.client.NotificationMsClient;
import com.rentalert.internal.client.OpsMsClient;
import com.rentalert.internal.client.ProcessingMsClient;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.internal.client.service.NotificationMsService;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.internal.client.service.impl.IdentityMsServiceImpl;
import com.rentalert.internal.client.service.impl.ListingMsServiceImpl;
import com.rentalert.internal.client.service.impl.NotificationMsServiceImpl;
import com.rentalert.internal.client.service.impl.OpsMsServiceImpl;
import com.rentalert.internal.client.service.impl.ProcessingMsServiceImpl;
import com.rentalert.internal.decoder.InternalErrorDecoder;
import com.rentalert.internal.interceptor.HeaderPropagationInterceptor;
import com.rentalert.internal.interceptor.InternalTokenInterceptor;
import com.rentalert.redis.service.CacheService;
import feign.Logger;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableFeignClients("com.rentalert")
@PropertySource("classpath:internal-integration-defaults.properties")
public class CommonFeignConfiguration {

    @Bean
    public Logger.Level feignLoggerLevel(@Value("${client.server.log-mode:false}") boolean fullLogging) {
        return fullLogging ? Logger.Level.FULL : Logger.Level.BASIC;
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

    @Bean
    @ConditionalOnMissingBean
    public ProcessingMsService processingMsService(@Lazy ProcessingMsClient processingMsClient,
        @Lazy CacheService cacheService, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ProcessingMsServiceImpl(processingMsClient, cacheService, retryRegistry, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ListingMsService listingMsService(@Lazy ListingMsClient listingMsClient, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ListingMsServiceImpl(listingMsClient, retryRegistry, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpsMsService opsMsService(@Lazy OpsMsClient opsMsClient, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        return new OpsMsServiceImpl(opsMsClient, retryRegistry, circuitBreakerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationMsService notificationMsService(@Lazy NotificationMsClient notificationMsClient,
        RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new NotificationMsServiceImpl(notificationMsClient, retryRegistry, circuitBreakerRegistry);
    }
}
