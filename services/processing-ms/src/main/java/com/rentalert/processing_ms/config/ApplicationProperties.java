package com.rentalert.processing_ms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private KufarProvider kufar = new KufarProvider();
    private Realt realt = new Realt();
    private Onliner onliner = new Onliner();
    private Currency currency = new Currency();

    @Getter
    @Setter
    public static class KufarProvider {
        private String url;
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
    public static class Currency {
        private String frankfurterUrl;
    }
}
