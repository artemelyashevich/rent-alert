package com.rentalert.notification_ms.config;

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

        /** Bot API URL with {@code %s} for the admin bot token. */
        private String url;
        private String pinUrl = "https://api.telegram.org/bot%s/pinChatMessage";
        private boolean botEnabled;
        private String botToken;
        private String webAppUrl;
        private String botName = "RentAlert";
        private String adminToken;
        private String adminChatId;
    }
}
