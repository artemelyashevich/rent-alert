package com.rentalert.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        log.info("[REQUEST] {} {}", request.getMethod(), request.getRequestURI());
        request.setAttribute(START_TIME, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {

        if (request.getAttribute(START_TIME) == null) {
            return;
        }
        long startTime = (long) request.getAttribute(START_TIME);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (ex == null) {
            log.info("[RESPONSE] {} {} -> status={} time={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        } else {
            log.error("[RESPONSE-FAILED] {} {} -> status={} time={}ms error={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    ex.toString(),
                    ex);
        }
    }
}