package com.rentalert.common.exception;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ErrorMessagesTests {

	private static final Locale RU = Locale.forLanguageTag("ru");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class));

	private Locale jvmLocale;

	@BeforeEach
	void pinJvmLocale() {
		jvmLocale = Locale.getDefault();
		Locale.setDefault(Locale.CHINESE);
	}

	@AfterEach
	void restoreJvmLocale() {
		Locale.setDefault(jvmLocale);
	}

	@ParameterizedTest
	@EnumSource(InternalExceptionCode.class)
	void everyCodeHasRussianAndEnglishMessage(InternalExceptionCode code) {
		contextRunner.run(context -> {
			MessageSource messages = context.getBean(MessageSource.class);
			String key = "ERROR_" + code.getLabel();

			assertThatCode(() -> messages.getMessage(key, null, RU)).as(key + " (ru)").doesNotThrowAnyException();
			assertThatCode(() -> messages.getMessage(key, null, Locale.ENGLISH)).as(key + " (en)").doesNotThrowAnyException();
		});
	}

	@Test
	void bootPicksUpBundleWithUtf8AndLocales() {
		contextRunner.run(context -> {
			MessageSource messages = context.getBean(MessageSource.class);

			assertThat(messages.getMessage("ERROR_ACCESS_DENIED", null, RU)).isEqualTo("Доступ запрещён");
			assertThat(messages.getMessage("ERROR_ACCESS_DENIED", null, Locale.ENGLISH)).isEqualTo("Access denied");
			// languages without their own bundle fall back to the default (English) one
			assertThat(messages.getMessage("ERROR_ACCESS_DENIED", null, Locale.GERMAN)).isEqualTo("Access denied");
		});
	}

	@Test
	void unknownKeyStillThrowsSoControllerCanLogIt() {
		contextRunner.run(context -> assertThatCode(() -> context.getBean(MessageSource.class).getMessage("ERROR_NOPE", null, RU))
				.isInstanceOf(NoSuchMessageException.class));
	}
}
