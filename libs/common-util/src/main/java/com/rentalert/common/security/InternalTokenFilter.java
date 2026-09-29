package com.rentalert.common.security;

import com.rentalert.common.config.InternalAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class InternalTokenFilter extends OncePerRequestFilter {

    private final RequestMatcher internalUrls;
    private final InternalAuthProperties internalAuthProperties;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !internalUrls.matches(request);
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isValid(request.getHeader(InternalAuth.HEADER))) {
            log.warn("[INTERNAL AUTH] Rejected {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            InternalAuth.AUTHORITY, null, List.of(new SimpleGrantedAuthority(InternalAuth.AUTHORITY))));
        filterChain.doFilter(request, response);
    }

    private boolean isValid(String presented) {
        String expected = internalAuthProperties.getToken();
        if (!StringUtils.hasText(expected) || presented == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
    }
}
