package com.rentalert.common.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rentalert.common.exception.UpstreamHttpException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestServiceImplTests {

    private static final String KUFAR = "https://api.kufar.by/search";
    private static final String REALT = "https://realt.by/bff/graphql";

    private MockRestServiceServer server;
    private RestServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RetryRegistry retries = RetryRegistry.of(RetryConfig.from(RestServiceImpl.defaultRetryConfig()).waitDuration(Duration.ofMillis(1)).build());
        CircuitBreakerRegistry circuitBreakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.from(RestServiceImpl.defaultCircuitBreakerConfig()).minimumNumberOfCalls(2).slidingWindowSize(2).build());
        service = new RestServiceImpl(builder.build(), retries, circuitBreakers);
    }

    @Test
    void retriesServerErrorOnce() {
        server.expect(once(), requestTo(KUFAR)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(once(), requestTo(KUFAR)).andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        assertThat(service.processGetRequest(KUFAR, String.class)).isEqualTo("ok");
        server.verify();
    }

    @Test
    void doesNotRetryClientError() {
        server.expect(once(), requestTo(KUFAR)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(UpstreamHttpException.class);
        server.verify();
    }

    @Test
    void openCircuitStopsCallsToThatHostOnly() {
        server.expect(times(4), requestTo(KUFAR)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(once(), requestTo(REALT)).andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(UpstreamHttpException.class);
        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(UpstreamHttpException.class);

        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(CallNotPermittedException.class);
        assertThat(service.processPostRequest(REALT, "{}", String.class)).isEqualTo("ok");
        server.verify();
    }

    @Test
    void rateLimitIsNotRetriedButCountsForCircuit() {
        server.expect(times(2), requestTo(KUFAR)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class))
            .isInstanceOfSatisfying(UpstreamHttpException.class, e -> assertThat(e.isRateLimited()).isTrue());
        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(UpstreamHttpException.class);

        assertThatThrownBy(() -> service.processGetRequest(KUFAR, String.class)).isInstanceOf(CallNotPermittedException.class);
        server.verify();
    }
}
