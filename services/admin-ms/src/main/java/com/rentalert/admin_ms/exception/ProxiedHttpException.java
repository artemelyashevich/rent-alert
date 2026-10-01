package com.rentalert.admin_ms.exception;

import com.rentalert.common.exception.UpstreamHttpException;
import lombok.Getter;

@Getter
public class ProxiedHttpException extends UpstreamHttpException {

    private final String body;

    public ProxiedHttpException(String message, int upstreamStatus, String body) {
        super(message, upstreamStatus, null);
        this.body = body;
    }
}
