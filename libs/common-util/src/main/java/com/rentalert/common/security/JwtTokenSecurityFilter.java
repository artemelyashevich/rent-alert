package com.rentalert.common.security;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.exception.ExceptionBody;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.common.i18n.I18nService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenSecurityFilter extends OncePerRequestFilter {

    private final RequestMatcher publicUrls;
    private final IpAccessGuard ipAccessGuard;
    private final I18nService i18nService;
    private final ObjectMapper objectMapper;
    private final AuthenticationManager authenticationManager;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String uri = request.getRequestURI();
        boolean internalCall = InternalAuth.isInternalCall();

        if (!internalCall && ipAccessGuard.isBlocked(ip)) {
            log.debug("[AUTH FILTER] Rejected request from blocked IP {} for {} {}", ip, request.getMethod(), uri);
            writeTooManyRequestsResponse(response);
            return;
        }
        log.debug("[AUTH FILTER] Incoming request: {} {} from {}", request.getMethod(), uri, ip);

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            if (!internalCall) {
                ipAccessGuard.registerRequestWithoutToken(ip);
            }
            log.debug("[AUTH FILTER] No Bearer token found for {} {} from {}", request.getMethod(), uri, ip);
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("[AUTH FILTER] Token found for {} {} -> authenticating", request.getMethod(), uri);
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(token, token));
            SecurityContextHolder.getContext().setAuthentication(
                internalCall ? InternalAuth.withInternalAuthority(authentication) : authentication);
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

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        boolean skip = publicUrls.matches(request);
        log.debug("[AUTH-FILTER] skip public url: {} {} -> {}", request.getMethod(), request.getRequestURI(), skip);
        return skip;
    }

    private void writeBlockedResponse(HttpServletResponse response) throws IOException {
        String message = i18nService.getMessage("ERROR_USER_BLOCKED");
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
        String message = i18nService.getErrorMessage(code.getLabel());
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
        String message = i18nService.getErrorMessage("ERROR_IP_BLOCKED");
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
