package com.rentalert.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String accessSecret, String refreshSecret, @DefaultValue("3600") long expirationSeconds) {
}
