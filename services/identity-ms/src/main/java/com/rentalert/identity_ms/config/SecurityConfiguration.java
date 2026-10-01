package com.rentalert.identity_ms.config;

import com.rentalert.common.security.AbstractSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfiguration extends AbstractSecurityConfiguration {

    @Override
    public RequestMatcher publicUrls() {
        return PublicUrls.PUBLIC_URLS;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
