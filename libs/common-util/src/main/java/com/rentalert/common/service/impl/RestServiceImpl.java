package com.rentalert.common.service.impl;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.common.service.RestService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class RestServiceImpl implements RestService {

    private static final String UNKNOWN_HOST = "unknown";

    private final RestClient restClient;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public RestServiceImpl(RestClient restClient) {
        this(restClient, RetryRegistry.of(defaultRetryConfig()), CircuitBreakerRegistry.of(defaultCircuitBreakerConfig()));
    }

    public RestServiceImpl(RestClient restClient, RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = restClient;
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public static RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(2))
            .retryOnException(RestServiceImpl::isTransient)
            .build();
    }

    public static CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(5))
            .permittedNumberOfCallsInHalfOpenState(2)
            .recordException(e -> isTransient(e) || e instanceof UpstreamHttpException upstream && upstream.isRateLimited())
            .build();
    }

    static boolean isTransient(Throwable e) {
        return e instanceof ResourceAccessException || e instanceof UpstreamHttpException upstream && upstream.isServerError();
    }

    static String hostOf(String uri) {
        try {
            String host = URI.create(uri).getHost();
            return host == null ? UNKNOWN_HOST : host;
        } catch (IllegalArgumentException e) {
            return UNKNOWN_HOST;
        }
    }

    private static void throwUpstreamError(String method, String uri, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        throw new UpstreamHttpException(
            "Error during " + method + " request: " + uri + ", status=" + status,
            status,
            parseRetryAfterSeconds(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        );
    }

    private static Long parseRetryAfterSeconds(String header) {
        if (header == null) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            // HTTP-date формат не поддерживаем — вызывающий код применит свой backoff.
            return null;
        }
    }

    @Override
    public <T> T processGetRequest(String uri, Class<T> responseType) {
        return execute(uri, () -> restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("GET", uri, response)
            )
            .body(responseType));
    }

    @Override
    public <T> T processGetRequest(String uri, ParameterizedTypeReference<T> typeRef) {
        return execute(uri, () -> restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("GET", uri, response)
            )
            .body(typeRef));
    }

    @Override
    public <T> T processPostRequest(String uri, String body, Class<T> responseType) {
        return execute(uri, () -> restClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("POST", uri, response)
            )
            .body(responseType));
    }

    @Override
    public String buildUri(String basePath, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(basePath);
        params.forEach(builder::queryParam);
        return builder.build().toUriString();
    }

    private <T> T execute(String uri, Supplier<T> call) {
        String host = hostOf(uri);
        Retry retry = retryRegistry.retry(host);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(host);
        return CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, call)).get();
    }
}
