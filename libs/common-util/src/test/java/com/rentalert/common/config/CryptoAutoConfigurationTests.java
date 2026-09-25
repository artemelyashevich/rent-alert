package com.rentalert.common.config;

import com.rentalert.common.auth.HmacBlindIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoAutoConfigurationTests {

	// Base64("key"); the expected hash is the RFC-style HMAC-SHA256 example value, Base64-encoded
	private static final String KEY = "a2V5";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CryptoAutoConfiguration.class));

	@Test
	void isRegisteredAsAutoConfiguration() {
		var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

		assertThat(candidates).contains(CryptoAutoConfiguration.class.getName());
	}

	@Test
	void bindsPropertiesAndHashesWithConfiguredKey() {
		contextRunner.withPropertyValues("app.crypto.hmac-key=" + KEY, "app.crypto.aes-key=aes").run(context -> {
			assertThat(context.getBean(CryptoProperties.class)).isEqualTo(new CryptoProperties("aes", KEY));

			HmacBlindIndexService service = context.getBean(HmacBlindIndexService.class);
			assertThat(service.hash("The quick brown fox jumps over the lazy dog")).isEqualTo("97yD9DBThCSxMpjmqm+xQ+9NWaFJRhdZl0edvC0aPNg=");
			assertThat(service.hash(null)).isNull();
		});
	}

	@Test
	void noServiceWhenKeyNotConfigured() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(HmacBlindIndexService.class));
	}

	@Test
	void failsFastOnEmptyKey() {
		contextRunner.withPropertyValues("app.crypto.hmac-key=").run(context -> assertThat(context).hasFailed()
				.getFailure().hasStackTraceContaining("app.crypto.hmac-key is empty"));
	}

	@Test
	void failsFastOnKeyThatIsNotBase64() {
		contextRunner.withPropertyValues("app.crypto.hmac-key=not base64!").run(context -> assertThat(context).hasFailed()
				.getFailure().hasStackTraceContaining("not valid Base64"));
	}
}
