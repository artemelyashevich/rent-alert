package com.rentalert.redis.config;

import com.rentalert.common.config.JwtAutoConfiguration;
import com.rentalert.common.security.AuthenticationService;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.UserSessionService;
import com.rentalert.redis.service.impl.SessionAuthenticationService;
import com.rentalert.redis.service.impl.UserSessionServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {RedisUtilAutoConfiguration.class, JwtAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({CacheService.class, JwtService.class})
public class UserSessionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UserSessionService userSessionService(CacheService cacheService, HttpServletRequest httpServletRequest, JwtService jwtService) {
        return new UserSessionServiceImpl(cacheService, httpServletRequest, jwtService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService authenticationService(JwtService jwtService, UserSessionService userSessionService) {
        return new SessionAuthenticationService(jwtService, userSessionService);
    }

}
