package com.rentalert.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static String traceIdOf(ServerHttpRequest request) {
        String incoming = request.getHeaders().getFirst(TRACE_ID_HEADER);
        return incoming != null && VALID_TRACE_ID.matcher(incoming).matches()
            ? incoming
            : UUID.randomUUID().toString();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = traceIdOf(request);
        String method = request.getMethod().name();
        String path = request.getPath().value();
        long startedAt = System.nanoTime();

        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
            return Mono.empty();
        });
        ServerHttpRequest traced = request.mutate().headers(headers -> headers.set(TRACE_ID_HEADER, traceId)).build();

        log.info("[REQUEST] {} {} traceId={}", method, path, traceId);
        return chain.filter(exchange.mutate().request(traced).build())
            .doFinally(signal -> log.info("[RESPONSE] {} {} -> status={} time={}ms traceId={}", method, path,
                exchange.getResponse().getStatusCode(), (System.nanoTime() - startedAt) / 1_000_000, traceId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
