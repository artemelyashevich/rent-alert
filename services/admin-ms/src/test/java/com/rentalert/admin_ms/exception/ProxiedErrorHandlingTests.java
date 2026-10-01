package com.rentalert.admin_ms.exception;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProxiedErrorHandlingTests {

    private final ProxiedErrorDecoder decoder = new ProxiedErrorDecoder();
    private final ProxiedExceptionHandler handler = new ProxiedExceptionHandler();

    @Test
    void clientErrorIsRelayedWithOwnerBody() {
        String body = "{\"message\":\"Город не найден\",\"details\":{\"errorCode\":\"404\"}}";

        ResponseEntity<?> response = handler.handleProxied(decode(404, body));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(body);
    }

    @Test
    void internalErrorBecomesBadGateway() {
        ResponseEntity<?> response = handler.handleProxied(decode(500, "{\"message\":\"internal details\"}"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(String.valueOf(response.getBody())).doesNotContain("internal details");
    }

    @Test
    void unavailableJobIsRelayed() {
        String body = "{\"message\":\"Задача отключена в этом окружении\"}";

        ResponseEntity<?> response = handler.handleProxied(decode(503, body));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(body);
    }

    @Test
    void clientErrorWithoutBodyKeepsStatus() {
        ResponseEntity<?> response = handler.handleProxied(decode(401, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ProxiedHttpException decode(int status, String body) {
        Request request = Request.create(Request.HttpMethod.GET, "http://processing-ms/x", Map.of(), null, StandardCharsets.UTF_8, null);
        Response.Builder response = Response.builder().status(status).reason("reason").request(request).headers(Map.of());
        if (body != null) {
            response.body(body, StandardCharsets.UTF_8);
        }
        return (ProxiedHttpException) decoder.decode("Client#call()", response.build());
    }
}
