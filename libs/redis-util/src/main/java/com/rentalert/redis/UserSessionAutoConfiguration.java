package com.rentalert.redis;

import com.rentalert.common.auth.JwtService;
import com.rentalert.common.config.JwtAutoConfiguration;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.UserSessionService;
import com.rentalert.redis.service.UserSessionServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

// Servlet services only: the session is looked up by the Authorization header of the current request
@AutoConfiguration(after = {RedisUtilAutoConfiguration.class, JwtAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({CacheService.class, JwtService.class})
public class UserSessionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public UserSessionService userSessionService(CacheService cacheService, HttpServletRequest httpServletRequest,
			JwtService jwtService) {
		return new UserSessionServiceImpl(cacheService, httpServletRequest, jwtService);
	}

}
