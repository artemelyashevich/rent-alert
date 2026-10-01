package com.rentalert.admin_ms.config;

import com.rentalert.common.security.AbstractSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfiguration extends AbstractSecurityConfiguration {

    @Override
    public RequestMatcher publicUrls() {
        return PublicUrls.PUBLIC_URLS;
    }
}
