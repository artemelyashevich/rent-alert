package com.rentalert.admin_ms.config;

import com.rentalert.admin_ms.exception.ProxiedErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    @Bean
    public ErrorDecoder proxiedErrorDecoder() {
        return new ProxiedErrorDecoder();
    }
}
