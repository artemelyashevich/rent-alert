package com.rentalert.internal.interceptor;

import com.rentalert.internal.annotation.IgnoreInterceptRequest;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class HeaderPropagationInterceptor implements RequestInterceptor {

    private static final List<String> PROPAGATED_HEADERS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.ACCEPT_LANGUAGE);

    @Override
    public void apply(RequestTemplate template) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        Optional<IgnoreInterceptRequest> ignore = findIgnoreAnnotation(template);

        for (String header : PROPAGATED_HEADERS) {
            if (isIgnored(ignore, header) || template.headers().containsKey(header)) {
                continue;
            }
            String value = request.getHeader(header);
            if (value != null) {
                template.header(header, value);
            }
        }
    }

    private static Optional<IgnoreInterceptRequest> findIgnoreAnnotation(RequestTemplate template) {
        if (template.methodMetadata() != null && template.methodMetadata().method() != null) {
            IgnoreInterceptRequest onMethod = template.methodMetadata().method().getAnnotation(IgnoreInterceptRequest.class);
            if (onMethod != null) {
                return Optional.of(onMethod);
            }
        }
        if (template.feignTarget() != null) {
            return Optional.ofNullable(template.feignTarget().type().getAnnotation(IgnoreInterceptRequest.class));
        }
        return Optional.empty();
    }

    private static boolean isIgnored(Optional<IgnoreInterceptRequest> ignore, String header) {
        return ignore
            .map(annotation -> annotation.headers().length == 0
                || Arrays.stream(annotation.headers()).anyMatch(header::equalsIgnoreCase))
            .orElse(false);
    }
}
