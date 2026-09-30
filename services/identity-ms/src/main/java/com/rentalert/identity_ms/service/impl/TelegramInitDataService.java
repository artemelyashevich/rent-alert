package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AuthException;
import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.identity_ms.config.ApplicationProperties;
import com.rentalert.identity_ms.service.TelegramInitDataParser;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
public class TelegramInitDataService implements TelegramInitDataParser {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEBAPP_DATA = "WebAppData";

    private final ApplicationProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    public TelegramUser verify(String initData) {
        Map<String, String> params = parseParams(initData);

        String receivedHash = extractHash(params);
        String dataCheckString = buildDataCheckString(params);
        String calculatedHash = calculateHash(dataCheckString);

        if (!constantTimeEquals(calculatedHash, receivedHash)) {
            throw new AuthException("Invalid initData hash");
        }

        validateAuthDate(params);

        return parseUser(params);
    }

    private Map<String, String> parseParams(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private String extractHash(Map<String, String> params) {
        String hash = params.remove("hash");
        if (hash == null || hash.isBlank()) {
            throw new AuthException("initData does not contain a hash");
        }
        return hash;
    }

    private String buildDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "\n" + b)
            .orElseThrow(() -> new AuthException("initData has no verifiable fields"));
    }

    private String calculateHash(String dataCheckString) {
        try {
            byte[] botTokenBytes = appProperties.getTelegram().getBotToken()
                .getBytes(StandardCharsets.UTF_8);
            byte[] secretKey = hmac(botTokenBytes, WEBAPP_DATA.getBytes(StandardCharsets.UTF_8));
            byte[] hashBytes = hmac(dataCheckString.getBytes(StandardCharsets.UTF_8), secretKey);
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new AuthException("Failed to compute HMAC: " + e.getMessage());
        }
    }

    private byte[] hmac(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key, HMAC_SHA256));
        return mac.doFinal(data);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private void validateAuthDate(Map<String, String> params) {
        String authDateStr = params.get("auth_date");
        if (authDateStr == null) {
            throw new AuthException("auth_date is missing from initData");
        }
        long authDate;
        try {
            authDate = Long.parseLong(authDateStr);
        } catch (NumberFormatException e) {
            throw new AuthException("auth_date is not a valid Unix timestamp");
        }
        long age = Instant.now().getEpochSecond() - authDate;
        long ttl = appProperties.getTelegram().getAuthDateTtlSeconds();
        if (age > ttl) {
            throw new AuthException(
                "initData is expired (age=" + age + "s, ttl=" + ttl + "s)");
        }
    }

    private TelegramUser parseUser(Map<String, String> params) {
        String userJson = params.get("user");
        if (userJson == null) {
            throw new AuthException("user field is missing from initData");
        }
        try {
            TelegramUser user = objectMapper.readValue(userJson, TelegramUser.class);
            if (user.getId() == null) {
                throw new AuthException("user.id is null");
            }
            return user;
        } catch (Exception e) {
            throw new AuthException("Failed to parse user JSON: " + e.getMessage());
        }
    }
}