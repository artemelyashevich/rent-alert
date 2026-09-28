package com.rentalert.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenSecurityFilter extends OncePerRequestFilter {

    public static final RequestMatcher SHOULD_NOT_FILTER = new OrRequestMatcher();

    private final IpAccessGuard ipAccessGuard;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        String ip = resolveClientIp(request);

        if (ipAccessGuard.isBlocked(ip)) {
            log.debug("[AUTH FILTER] Rejected request from blocked IP {} for {} {}", ip, request.getMethod(), uri);
            writeTooManyRequestsResponse(response);
            return;
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        boolean skip = SHOULD_NOT_FILTER.matches(request);
        log.debug("[AUTH-FILTER] skip public url: {} {} -> {}", request.getMethod(), request.getRequestURI(), skip);
        return skip;
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        String message = messageSource.getMessage("ERROR_IP_BLOCKED", null, Locale.forLanguageTag("ru"));
        ExceptionBody body = ExceptionBody.builder()
                .message(message)
                .details(Map.of("errorCode", "IP_BLOCKED"))
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
