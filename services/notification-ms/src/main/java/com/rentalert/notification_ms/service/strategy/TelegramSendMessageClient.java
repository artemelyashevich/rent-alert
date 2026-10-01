package com.rentalert.notification_ms.service.strategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppRateLimitException;
import com.rentalert.notification_ms.config.ApplicationProperties;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;


@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramSendMessageClient {

    private static final String PARSE_MODE = "HTML";

    private final ApplicationProperties applicationProperties;
    private final RestClient restClient;

    public void send(String botToken, String chatId, String text) {
        call(() -> request(applicationProperties.getTelegram().getUrl().formatted(botToken), messageParams(chatId, text), text)
            .toBodilessEntity());
    }

    public void sendAndPin(String botToken, String chatId, String text) {
        SendMessageResponse response = call(() -> request(applicationProperties.getTelegram().getUrl().formatted(botToken),
            messageParams(chatId, text), text).body(SendMessageResponse.class));
        if (response == null || response.result() == null || response.result().messageId() == null) {
            log.warn("[TELEGRAM] No message id in sendMessage response, message is not pinned. chat={}", chatId);
            return;
        }
        MultiValueMap<@NonNull String, String> params = new LinkedMultiValueMap<>();
        params.add("chat_id", chatId);
        params.add("message_id", String.valueOf(response.result().messageId()));
        params.add("disable_notification", "true");
        call(() -> request(applicationProperties.getTelegram().getPinUrl().formatted(botToken), params, text).toBodilessEntity());
    }

    private MultiValueMap<@NonNull String, String> messageParams(String chatId, String text) {
        MultiValueMap<@NonNull String, String> params = new LinkedMultiValueMap<>();
        params.add("chat_id", chatId);
        params.add("text", text);
        params.add("parse_mode", PARSE_MODE);
        return params;
    }

    private RestClient.ResponseSpec request(String url, MultiValueMap<@NonNull String, String> params, String text) {
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(params)
            .retrieve()
            .onStatus(
                status -> status.value() == 429,
                (request, response) -> {
                    String retryAfterHeader = response.getHeaders().getFirst("Retry-After");
                    Long retryAfterSeconds = parseRetryAfter(retryAfterHeader);
                    log.warn("[TELEGRAM] Rate limited. Retry-After: {}s", retryAfterHeader);
                    throw new AppRateLimitException(
                        "[TELEGRAM]: too many requests", retryAfterSeconds);
                }
            )
            .onStatus(
                status -> status.value() == 400,
                (request, response) -> {
                    log.error("[TELEGRAM] Bad request, possibly malformed {} markup. body={}",
                        PARSE_MODE, text);
                    throw new AppException("[TELEGRAM]: bad request", HttpStatus.BAD_REQUEST);
                }
            )
            .onStatus(
                status -> status.value() == 403,
                (request, response) -> {
                    log.warn("[TELEGRAM] Forbidden: the user blocked the bot or never started it");
                    throw new AppException("[TELEGRAM]: forbidden", HttpStatus.FORBIDDEN);
                }
            );
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to send telegram notification", ex);
            throw new AppException(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            log.debug("Non-numeric Retry-After header: {}", header);
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendMessageResponse(Message result) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("message_id") Long messageId) {

    }
}
