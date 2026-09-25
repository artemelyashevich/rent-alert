package com.rentalert.common.config;

import com.rentalert.common.auth.JwtService;
import com.rentalert.common.auth.JwtType;
import com.rentalert.common.exception.AuthException;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAutoConfigurationTests {

	private static final String ACCESS = "test-access-secret-that-is-at-least-32-bytes";
	private static final String REFRESH = "test-refresh-secret-that-is-at-least-32-bytes";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JwtAutoConfiguration.class));

	@Test
	void isRegisteredAsAutoConfiguration() {
		var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

		assertThat(candidates).contains(JwtAutoConfiguration.class.getName());
	}

	@Test
	void generatedAccessTokenCarriesUserIdAndExpiration() {
		contextRunner.withPropertyValues("app.auth.access-secret=" + ACCESS, "app.auth.refresh-secret=" + REFRESH,
				"app.auth.expiration-seconds=600").run(context -> {
			JwtService jwt = context.getBean(JwtService.class);

			String token = jwt.generateToken(42L, JwtType.ACCESS);

			assertThat(jwt.extractUserId(token)).isEqualTo(42L);
			long secondsLeft = (jwt.extractExpiration(token).getTime() - new Date().getTime()) / 1000;
			assertThat(secondsLeft).isBetween(590L, 600L);
		});
	}

	@Test
	void refreshTokenIsNotAcceptedAsAccessToken() {
		contextRunner.withPropertyValues("app.auth.access-secret=" + ACCESS, "app.auth.refresh-secret=" + REFRESH).run(context -> {
			JwtService jwt = context.getBean(JwtService.class);
			String refresh = jwt.generateToken(1L, JwtType.REFRESH);

			jwt.validateToken(refresh, JwtType.REFRESH);
			assertThatThrownBy(() -> jwt.extractUserId(refresh)).isInstanceOf(AuthException.class);
		});
	}

	@Test
	void noServiceWithoutAccessSecret() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(JwtService.class));
	}

	@Test
	void failsFastOnShortSecret() {
		contextRunner.withPropertyValues("app.auth.access-secret=short", "app.auth.refresh-secret=" + REFRESH)
				.run(context -> assertThat(context).hasFailed().getFailure()
						.hasStackTraceContaining("app.auth.access-secret must be set and at least 32 bytes long"));
	}
}
