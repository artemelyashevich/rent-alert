package com.rentalert.admin_ms.exception;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxiedErrorDecoder implements ErrorDecoder {

    private static String readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream body = response.body().asInputStream()) {
            return Util.toString(new InputStreamReader(body, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Failed to read error body: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        String message = "%s returned %d: %s".formatted(methodKey, response.status(), body);
        return new ProxiedHttpException(message, response.status(), body);
    }
}
