package com.rentalert.identity_ms.security;

import com.bsu.renalert.infra.domain.dto.exception.ExceptionBody;
import com.bsu.renalert.infra.domain.entity.InternalExceptionCode;
import com.bsu.renalert.infra.exception.AuthException;
import com.bsu.renalert.infra.exception.UserBlockedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final RequestMatcher protectedUrls;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final IpAccessGuard ipAccessGuard;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        boolean skip = !protectedUrls.matches(request);
        log.debug("[AUTH FILTER] shouldNotFilter: {} {} -> {}", request.getMethod(), request.getRequestURI(), skip);
        return skip;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String ip = resolveClientIp(request);

        if (ipAccessGuard.isBlocked(ip)) {
            log.debug("[AUTH FILTER] Rejected request from blocked IP {} for {} {}", ip, request.getMethod(), uri);
            writeTooManyRequestsResponse(response);
            return;
        }

        log.debug("[AUTH FILTER] Incoming request: {} {} from {}", request.getMethod(), uri, ip);
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            ipAccessGuard.registerRequestWithoutToken(ip);
            log.debug("[AUTH FILTER] No Bearer token found for {} {} from {}", request.getMethod(), uri, ip);
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("[AUTH FILTER] Token found for {} {} -> authenticating", request.getMethod(), uri);
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(token, token));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("[AUTH FILTER] Authentication SUCCESS for {} {}", request.getMethod(), uri);
        } catch (UserBlockedException ex) {
            SecurityContextHolder.clearContext();
            log.warn("[AUTH FILTER] Blocked user attempted access for {} {}", request.getMethod(), uri);
            writeBlockedResponse(response);
            return;
        } catch (AuthException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            log.warn("[AUTH FILTER] Authentication FAILED for {} {}: {}", request.getMethod(), uri, ex.getMessage());
            writeUnauthorizedResponse(response);
            return;
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("[AUTH FILTER] Authentication FAILED for {} {}: {}", request.getMethod(), uri, ex.getMessage());
            throw ex;
        }
        filterChain.doFilter(request, response);
    }

    private void writeBlockedResponse(HttpServletResponse response) throws IOException {
        String message = messageSource.getMessage("ERROR_USER_BLOCKED", null, Locale.forLanguageTag("ru"));
        ExceptionBody body = ExceptionBody.builder()
            .message(message)
            .details(Map.of("errorCode", "USER_BLOCKED"))
            .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        InternalExceptionCode code = InternalExceptionCode.EX401;
        String message = messageSource.getMessage("ERROR_" + code.getLabel(), null, Locale.forLanguageTag("ru"));
        ExceptionBody body = ExceptionBody.builder()
            .message(message)
            .details(Map.of("errorCode", String.valueOf(code.getCode())))
            .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
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

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            log.debug("[AUTH FILTER] Authorization header is missing");
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            log.debug("[AUTH FILTER] Authorization header is not Bearer token");
            return null;
        }
        return header.substring(7);
    }
}
