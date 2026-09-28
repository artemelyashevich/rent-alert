package com.rentalert.common.security;

import com.rentalert.common.common.I18nService;
import com.rentalert.common.exception.ExceptionBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final IpAccessGuard ipAccessGuard;
    private final ObjectMapper objectMapper;
    private final I18nService i18nService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        if (ipAccessGuard.isRateLimited(ip)) {
            log.debug("[RATE LIMIT] Rejected {} {} from {}", request.getMethod(), request.getRequestURI(), ip);
            writeTooManyRequestsResponse(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        String message = i18nService.getErrorMessage("ERROR_RATE_LIMITED");
        ExceptionBody body = ExceptionBody.builder()
                .message(message)
                .details(Map.of("errorCode", "RATE_LIMITED"))
                .build();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}