package com.rentalert.realtime_ms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.redis.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketConfigurationTests {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(WebSocketConfiguration.class, WebSocketAuthChannelInterceptor.class)
        .withBean(CacheService.class, () -> mock(CacheService.class))
        .withBean(JwtService.class, () -> mock(JwtService.class));

    @Test
    void brokerStarts() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SimpMessagingTemplate.class);
        });
    }
}
