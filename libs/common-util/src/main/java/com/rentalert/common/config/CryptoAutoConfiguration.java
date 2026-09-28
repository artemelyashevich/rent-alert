package com.rentalert.common.config;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    @Bean
    @ConditionalOnProperty("app.crypto.hmac-key")
    @ConditionalOnMissingBean
    public HmacBlindIndexService hmacBlindIndexService(CryptoProperties cryptoProperties) {
        return new HmacBlindIndexService(cryptoProperties);
    }

    @Bean
    @ConditionalOnProperty("app.crypto.aes-key")
    @ConditionalOnMissingBean
    public CryptoService cryptoService(CryptoProperties cryptoProperties) {
        return new CryptoService(cryptoProperties);
    }
}
