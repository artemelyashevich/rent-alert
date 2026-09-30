package com.rentalert.notification_ms.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.notification_ms.config.TelegramBotConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.TelegramBotsApi;

class TelegramBotToggleTests {

    // No TelegramMessageService bean on purpose: the disabled bot must not require it
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TelegramBotConfiguration.class, TelegramBot.class);

    @Test
    void botIsOffByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TelegramBot.class);
            assertThat(context).doesNotHaveBean(TelegramBotsApi.class);
        });
    }

    @Test
    void botIsOffWhenDisabled() {
        runner.withPropertyValues("app.telegram.bot-enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TelegramBot.class);
        });
    }
}
