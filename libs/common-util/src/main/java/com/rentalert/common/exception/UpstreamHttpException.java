package com.rentalert.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Внешний сервис ответил не-2xx. Для клиентов нашего API это по-прежнему 500,
 * а исходный статус и Retry-After доступны вызывающему коду для ретраев и backoff.
 */
@Getter
public class UpstreamHttpException extends AppException {

    private final int upstreamStatus;
    private final Long retryAfterSeconds;

    public UpstreamHttpException(String message, int upstreamStatus, Long retryAfterSeconds) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.upstreamStatus = upstreamStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isRateLimited() {
        return upstreamStatus == HttpStatus.TOO_MANY_REQUESTS.value();
    }

    public boolean isServerError() {
        return upstreamStatus >= 500;
    }
}
