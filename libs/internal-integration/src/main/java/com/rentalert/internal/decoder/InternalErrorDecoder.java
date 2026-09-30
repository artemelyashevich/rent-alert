package com.rentalert.internal.decoder;

import com.rentalert.common.exception.ExceptionBody;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.common.util.JsonHelper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;


@Slf4j
public class InternalErrorDecoder implements ErrorDecoder {

    private static String readMessage(Response response) {
        if (response.body() == null) {
            return response.reason();
        }
        try (InputStream body = response.body().asInputStream()) {
            String raw = Util.toString(new InputStreamReader(body, StandardCharsets.UTF_8));
            if (JsonHelper.fromJson(raw, ExceptionBody.class) instanceof ExceptionBody exceptionBody
                && exceptionBody.getMessage() != null) {
                return exceptionBody.getMessage();
            }
            return raw;
        } catch (IOException e) {
            log.warn("Failed to read error body: {}", e.getMessage());
            return response.reason();
        }
    }

    private static Long parseRetryAfter(Response response) {
        Collection<String> values = response.headers().get(HttpHeaders.RETRY_AFTER);
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(values.iterator().next().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "%s returned %d: %s".formatted(methodKey, response.status(), readMessage(response));
        return new UpstreamHttpException(message, response.status(), parseRetryAfter(response));
    }
}
