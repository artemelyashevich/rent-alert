package com.rentalert.common.auth;

import com.rentalert.common.config.CryptoProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacBlindIndexService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public HmacBlindIndexService(CryptoProperties cryptoProperties) {
        this.key = new SecretKeySpec(decodeKey(cryptoProperties.hmacKey()), ALGORITHM);
    }

    public String hash(String value) {
        if (value == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] result = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка вычисления HMAC", e);
        }
    }

    private static byte[] decodeKey(String hmacKey) {
        if (hmacKey == null || hmacKey.isBlank()) {
            throw new IllegalStateException("app.crypto.hmac-key is empty: set it (Base64) for this service");
        }
        try {
            return Base64.getDecoder().decode(hmacKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.crypto.hmac-key is not valid Base64", e);
        }
    }
}
