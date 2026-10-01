package com.rentalert.admin_ms.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rentalert.admin_ms.exception.ProxiedHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.internal.client.ProcessingMsClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "app.kafka.outbox.enabled=false",
    "app.auth.access-secret=test-access-secret-0123456789abcdef",
    "app.auth.refresh-secret=test-refresh-secret-0123456789abcdef",
    "app.internal.token=test-internal-token"
})
class ProcessingMsClientTests {

    private static final AtomicReference<String> LAST_QUERY = new AtomicReference<>();
    private static final AtomicReference<String> LAST_TOKEN = new AtomicReference<>();
    private static final HttpServer SERVER = startServer();

    @Autowired
    private ProcessingMsClient client;

    @DynamicPropertySource
    static void processingUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.processing-ms.url",
            () -> "http://localhost:" + SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/processing/internal/admin/cities", exchange -> {
                LAST_QUERY.set(exchange.getRequestURI().getRawQuery());
                LAST_TOKEN.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
                boolean missing = exchange.getRequestURI().getPath().endsWith("/404");
                String body = missing
                    ? "{\"message\":\"Город не найден\"}"
                    : "{\"content\":[{\"id\":1,\"provider\":\"KUFAR\",\"name\":\"Минск\",\"cityCode\":\"minsk\",\"active\":true}],"
                        + "\"number\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(missing ? 404 : 200, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void pageIsRequestedAndDecoded() {
        PageResponse<CityResponse> page = client.findCities(0, 20, List.of("provider,ASC", "name,ASC"));

        assertThat(LAST_QUERY.get()).isEqualTo("page=0&size=20&sort=provider%2CASC&sort=name%2CASC");
        assertThat(LAST_TOKEN.get()).isEqualTo("test-internal-token");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).singleElement()
            .satisfies(city -> {
                assertThat(city.provider()).isEqualTo(ScheduleProvider.KUFAR);
                assertThat(city.name()).isEqualTo("Минск");
            });
    }

    @Test
    void ownerErrorKeepsStatusAndBody() {
        assertThatThrownBy(() -> client.findCity(404L))
            .isInstanceOf(ProxiedHttpException.class)
            .satisfies(e -> {
                ProxiedHttpException proxied = (ProxiedHttpException) e;
                assertThat(proxied.getUpstreamStatus()).isEqualTo(404);
                assertThat(proxied.getBody()).contains("Город не найден");
            });
    }
}
