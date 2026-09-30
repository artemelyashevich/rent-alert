package com.rentalert.common.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 10_000;

    private static final List<String> EXCLUDED_PATTERNS = List.of(
            "/api/v1/auth/**",
            "/api/login",
            "/api/register",
            "/api/token/refresh"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!log.isDebugEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return EXCLUDED_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequestBody(wrappedRequest);
            logResponseBody(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) {
            return;
        }
        String payload = getPayload(buf, request.getCharacterEncoding());
        log.debug("[REQUEST BODY] {} {} -> {}", request.getMethod(), request.getRequestURI(), payload);
    }

    private void logResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) {
            return;
        }
        String payload = getPayload(buf, response.getCharacterEncoding());
        log.debug("[RESPONSE BODY] status={} -> {}", response.getStatus(), payload);
    }

    private String getPayload(byte[] buf, String encoding) {
        int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
        try {
            String charset = encoding != null ? encoding : StandardCharsets.UTF_8.name();
            String content = new String(buf, 0, length, charset);
            return buf.length > MAX_PAYLOAD_LENGTH ? content + "...[truncated]" : content;
        } catch (Exception e) {
            return "[unreadable body]";
        }
    }
}