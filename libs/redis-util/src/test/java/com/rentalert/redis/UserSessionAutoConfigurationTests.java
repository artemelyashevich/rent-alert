package com.rentalert.redis;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.security.AuthenticationService;
import com.rentalert.redis.config.UserSessionAutoConfiguration;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.impl.SessionAuthenticationService;
import com.rentalert.redis.service.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserSessionAutoConfigurationTests {

	@Test
	void isRegisteredAsAutoConfiguration() {
		var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

		assertThat(candidates).contains(UserSessionAutoConfiguration.class.getName());
	}

	@Test
	void registeredInServletAppWithCacheAndJwt() {
		new WebApplicationContextRunner()
				.withBean(CacheService.class, () -> mock(CacheService.class))
				.withBean(JwtService.class, () -> mock(JwtService.class))
				.withConfiguration(AutoConfigurations.of(UserSessionAutoConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(UserSessionService.class);
					assertThat(context).getBean(AuthenticationService.class).isInstanceOf(SessionAuthenticationService.class);
				});
	}

	@Test
	void skippedWithoutJwtService() {
		new WebApplicationContextRunner()
				.withBean(CacheService.class, () -> mock(CacheService.class))
				.withConfiguration(AutoConfigurations.of(UserSessionAutoConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(UserSessionService.class));
	}

	@Test
	void skippedInReactiveApps() {
		new ReactiveWebApplicationContextRunner()
				.withBean(CacheService.class, () -> mock(CacheService.class))
				.withBean(JwtService.class, () -> mock(JwtService.class))
				.withConfiguration(AutoConfigurations.of(UserSessionAutoConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(UserSessionService.class));
	}
}
