package com.rentalert.identity_ms.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Telegram telegram = new Telegram();
    private KufarProvider kufar = new KufarProvider();
    private Realt realt = new Realt();
    private Onliner onliner = new Onliner();
    private Auth auth = new Auth();
    private Security security = new Security();
    private Crypto crypto = new Crypto();
    private Currency currency = new Currency();
    private Cache cache = new Cache();
    private WebSocket websocket = new WebSocket();

    @Getter
    @Setter
    public static class WebSocket {
        private boolean enabled = true;
        private String endpoint = "/ws";
    }

    @Getter
    @Setter
    public static class Cache {
        private int otpTtlSeconds = 60;
    }

    @Getter
    @Setter
    public static class Realt {
        private String url;
    }

    @Getter
    @Setter
    public static class Onliner {
        private String url;
    }

    @Getter
    @Setter
    public static class Crypto {
        private String aesKey;
        private String hmacKey;
    }

    @Getter
    @Setter
    public static class Telegram {
        private String url;
        private String adminName;
        private String adminToken;
        private String botToken;
        private String botName = "RentAlert";
        private String adminChatId;
        private long authDateTtlSeconds;
    }


    @Getter
    @Setter
    public static class Security {
        private String superUserEmail;
        private String superUserPassword;
        private String oauthRedirectUrl;
        private List<String> frontAddresses;
        private String allowedMethods;
        private String allowedHeaders;
        private Boolean allowCredentials;
        private IpRateLimit ipRateLimit = new IpRateLimit();
        private RequestRateLimit rateLimit = new RequestRateLimit();
    }

    @Getter
    @Setter
    public static class IpRateLimit {
        private boolean enabled = true;
        private int maxRequestsWithoutToken = 30;
        private long windowSeconds = 60;
        private long blockDurationSeconds = 900;
    }

    @Getter
    @Setter
    public static class RequestRateLimit {
        private boolean enabled = true;
        private int requestsPerWindow = 60;
        private long windowSeconds = 10;
    }

    @Getter
    @Setter
    public static class KufarProvider {
        private String url;
        private String countUrl;
    }

    @Getter
    @Setter
    public static class Auth {
        private String accessSecret;
        private String refreshSecret;
        private long expirationSeconds = 3600;
    }

    @Getter
    @Setter
    public static class Currency {
        private String frankfurterUrl;
    }
}
