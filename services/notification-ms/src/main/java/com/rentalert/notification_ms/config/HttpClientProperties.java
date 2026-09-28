package com.rentalert.notification_ms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.http.client")
public class HttpClientProperties {

    private int maxConnections = 200;

    private int maxConnectionsPerRoute = 50;

    private long connectTimeoutMs = 5000;

    private long connectionRequestTimeoutMs = 3000;

    private long readTimeoutMs = 15000;

    private long idleEvictSeconds = 30;

    private long validateAfterInactivitySeconds = 5;

    private long connectionTtlMinutes = 5;
}