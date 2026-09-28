package com.rentalert.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ip-rate-limit")
public class IpRateLimitProperties {
    private boolean enabled = true;
    private int maxRequestsWithoutToken = 30;
    private long windowSeconds = 60;
    private long blockDurationSeconds = 900;
}