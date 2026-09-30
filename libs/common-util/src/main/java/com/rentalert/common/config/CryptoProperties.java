package com.rentalert.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crypto")
public record CryptoProperties(String aesKey, String hmacKey) {

}
