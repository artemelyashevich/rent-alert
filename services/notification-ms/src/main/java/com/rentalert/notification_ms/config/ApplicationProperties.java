package com.rentalert.notification_ms.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Telegram telegram = new Telegram();
    private Startup startup = new Startup();

    @Getter
    @Setter
    public static class Startup {

        /** Services that must all report a start before the admin is told the application is ready. */
        private List<String> services = List.of();
    }

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
