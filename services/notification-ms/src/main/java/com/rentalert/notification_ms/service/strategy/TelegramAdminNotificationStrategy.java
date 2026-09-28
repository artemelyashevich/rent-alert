package com.rentalert.notification_ms.service.strategy;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppRateLimitException;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.NotificationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAdminNotificationStrategy implements NotificationStrategy {

    private static final String PARSE_MODE = "HTML";

    private final ApplicationProperties applicationProperties;
    private final RestClient restClient;

    private String url;

    @PostConstruct
    public void init() {
        this.url = applicationProperties.getTelegram()
            .getUrl()
            .formatted(applicationProperties.getTelegram().getAdminToken());
    }

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        MultiValueMap<@NonNull String, String> params = new LinkedMultiValueMap<>();
        params.add(
            "chat_id",
            applicationProperties.getTelegram().getAdminChatId()
        );
        params.add(
            "text",
            notificationDto.getContent()
        );
        params.add("parse_mode", PARSE_MODE);
        try {
            restClient.post()
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
                                    PARSE_MODE, notificationDto.getContent());
                            throw new AppException("[TELEGRAM]: bad request", HttpStatus.BAD_REQUEST);
                        }
                )
                .toBodilessEntity();

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

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM_ADMIN;
    }
}