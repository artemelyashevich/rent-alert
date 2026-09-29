package com.rentalert.internal.interceptor;

import com.rentalert.common.security.InternalAuth;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;

public class InternalTokenInterceptor implements RequestInterceptor {

    private final String token;

    public InternalTokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (StringUtils.hasText(token)) {
            template.header(InternalAuth.HEADER, token);
        }
    }
}
