package com.rentalert.gateway.filter;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdGlobalFilterTests {

    private final TraceIdGlobalFilter filter = new TraceIdGlobalFilter();

    @Test
    void forwardsIncomingTraceIdAndReturnsIt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/listings").header(TraceIdGlobalFilter.TRACE_ID_HEADER, "abc-123"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return next.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_ID_HEADER))
            .isEqualTo("abc-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_ID_HEADER))
            .isEqualTo("abc-123");
    }

    @Test
    void generatesTraceIdWhenMissingOrMalformed() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/listings").header(TraceIdGlobalFilter.TRACE_ID_HEADER, "bad value"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(TraceIdGlobalFilter.TRACE_ID_HEADER))
            .matches("[0-9a-f-]{36}");
    }
}
