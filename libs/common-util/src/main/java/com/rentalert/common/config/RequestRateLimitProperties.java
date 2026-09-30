package com.rentalert.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RequestRateLimitProperties {

    private boolean enabled = true;
    private int requestsPerWindow = 60;
    private long windowSeconds = 10;
}