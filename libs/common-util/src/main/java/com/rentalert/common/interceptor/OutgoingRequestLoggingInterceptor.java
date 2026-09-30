package com.rentalert.common.interceptor;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class OutgoingRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Pattern BOT_TOKEN = Pattern.compile("/bot[^/]+");

    static String describe(URI uri) {
        String path = uri.getRawPath() == null ? "" : BOT_TOKEN.matcher(uri.getRawPath()).replaceFirst("/bot***");
        return uri.getHost() + path;
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String target = describe(request.getURI());
        long startedAt = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("[OUTGOING] {} {} -> status={} time={}ms", request.getMethod(), target, response.getStatusCode().value(), elapsedMs(startedAt));
            return response;
        } catch (IOException | RuntimeException e) {
            log.warn("[OUTGOING-FAILED] {} {} -> time={}ms error={}", request.getMethod(), target, elapsedMs(startedAt), e.toString());
            throw e;
        }
    }
}
