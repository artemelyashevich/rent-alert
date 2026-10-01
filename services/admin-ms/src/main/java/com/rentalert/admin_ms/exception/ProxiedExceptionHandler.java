package com.rentalert.admin_ms.exception;

import com.rentalert.common.exception.ExceptionBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxiedExceptionHandler {

    @ExceptionHandler(ProxiedHttpException.class)
    public ResponseEntity<?> handleProxied(ProxiedHttpException e) {
        boolean internalError = e.getUpstreamStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (internalError || !StringUtils.hasText(e.getBody())) {
            log.error("Upstream call failed: {}", e.getMessage());
            HttpStatus status = e.isServerError() ? HttpStatus.BAD_GATEWAY : HttpStatus.valueOf(e.getUpstreamStatus());
            return ResponseEntity.status(status).body(ExceptionBody.builder().message(status.getReasonPhrase()).build());
        }
        return ResponseEntity.status(e.getUpstreamStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(e.getBody());
    }
}
