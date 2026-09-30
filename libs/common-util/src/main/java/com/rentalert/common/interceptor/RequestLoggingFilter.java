package com.rentalert.common.interceptor;

import com.rentalert.dto.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    static String traceIdOf(HttpServletRequest request) {
        String incoming = request.getHeader(GlobalConstants.TRACE_ID_HEADER);
        return incoming != null && VALID_TRACE_ID.matcher(incoming).matches()
            ? incoming
            : UUID.randomUUID().toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = traceIdOf(request);
        MDC.put(GlobalConstants.TRACE_ID, traceId);
        response.setHeader(GlobalConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(GlobalConstants.TRACE_ID);
        }
    }
}
