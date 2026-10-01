package com.rentalert.notification_ms.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppRateLimitException;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.BotBlockedService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramNotificationStrategiesTests {

    private final BotBlockedService botBlockedService = mock(BotBlockedService.class);
    private MockRestServiceServer server;
    private TelegramAdminNotificationStrategy adminStrategy;
    private TelegramBotNotificationStrategy botStrategy;

    private static NotificationDto dto(String content, String receiver) {
        NotificationDto dto = new NotificationDto();
        dto.setContent(content);
        dto.setReceiver(receiver);
        return dto;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getTelegram().setUrl("https://api.telegram.org/bot%s/sendMessage");
        properties.getTelegram().setAdminToken("admin-token");
        properties.getTelegram().setAdminChatId("-100");
        properties.getTelegram().setBotToken("bot-token");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        TelegramSendMessageClient client = new TelegramSendMessageClient(properties, builder.build());
        adminStrategy = new TelegramAdminNotificationStrategy(properties, client);
        botStrategy = new TelegramBotNotificationStrategy(properties, client, botBlockedService);
    }

    @Test
    void marksMessagesForTheSharedAdminChannel() {
        server.expect(requestTo("https://api.telegram.org/botadmin-token/sendMessage"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("chat_id=-100"),
                containsString("text=" + encode("[MICROSERVICE] New listing")))))
            .andRespond(withSuccess());

        adminStrategy.sendNotification(dto("New listing", null));

        server.verify();
    }

    @Test
    void sendsUserMessageWithMainBotToUsersChatWithoutPrefix() {
        server.expect(requestTo("https://api.telegram.org/botbot-token/sendMessage"))
            .andExpect(content().string(allOf(
                containsString("chat_id=12345"),
                containsString("text=" + encode("New listing")),
                containsString("parse_mode=HTML"),
                not(containsString("MICROSERVICE")))))
            .andRespond(withSuccess());

        botStrategy.sendNotification(dto("New listing", "12345"));

        server.verify();
    }

    @Test
    void reportsBlockedBotAsForbidden() {
        server.expect(requestTo("https://api.telegram.org/botbot-token/sendMessage"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        NotificationDto dto = dto("New listing", "12345");
        dto.setUserId(42L);

        assertThatThrownBy(() -> botStrategy.sendNotification(dto))
            .isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(botBlockedService).handle(42L);
    }

    @Test
    void passesRetryAfterOnRateLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "17");
        server.expect(requestTo("https://api.telegram.org/botbot-token/sendMessage"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

        assertThatThrownBy(() -> botStrategy.sendNotification(dto("New listing", "12345")))
            .isInstanceOfSatisfying(AppRateLimitException.class,
                e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(17L));
        verify(botBlockedService, never()).handle(any());
    }

    @Test
    void pinsBroadcastMessage() {
        server.expect(requestTo("https://api.telegram.org/botbot-token/sendMessage"))
            .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":77,\"chat\":{\"id\":12345}}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.telegram.org/botbot-token/pinChatMessage"))
            .andExpect(content().string(allOf(containsString("chat_id=12345"), containsString("message_id=77"))))
            .andRespond(withSuccess());
        NotificationDto dto = dto("Broadcast", "12345");
        dto.setPin(true);

        botStrategy.sendNotification(dto);

        server.verify();
    }
}
