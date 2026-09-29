package com.rentalert.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.internal")
public class InternalAuthProperties {

    /** Shared secret of service-to-service calls. When empty, every internal request is rejected. */
    private String token;
}
