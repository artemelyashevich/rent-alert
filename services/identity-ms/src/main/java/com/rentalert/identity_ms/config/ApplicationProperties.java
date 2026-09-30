package com.rentalert.identity_ms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Telegram telegram = new Telegram();

    @Getter
    @Setter
    public static class Telegram {

        private String botToken;
        private long authDateTtlSeconds = 3600;
    }
}
