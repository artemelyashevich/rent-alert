package com.rentalert.common.config;

import com.rentalert.common.auth.JwtService;
import com.rentalert.common.auth.JwtServiceImpl;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Jwts.class)
@EnableConfigurationProperties(AuthProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnProperty("app.auth.access-secret")
    @ConditionalOnMissingBean
    public JwtService jwtService(AuthProperties authProperties) {
        return new JwtServiceImpl(authProperties);
    }
}
