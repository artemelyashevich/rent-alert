package com.rentalert.internal.decoder;

import com.rentalert.common.exception.UpstreamHttpException;
import feign.Request;
import feign.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class InternalErrorDecoderTests {

    private final InternalErrorDecoder decoder = new InternalErrorDecoder();

    @Test
    void takesMessageFromExceptionBody() {
        Exception exception = decoder.decode("IdentityMsClient#getUser(UUID)",
                response(404, "{\"message\":\"User not found\"}", Map.of()));

        assertThat(exception).isInstanceOfSatisfying(UpstreamHttpException.class, upstream -> {
            assertThat(upstream.getUpstreamStatus()).isEqualTo(404);
            assertThat(upstream.getRetryAfterSeconds()).isNull();
            assertThat(upstream.getMessage()).isEqualTo("IdentityMsClient#getUser(UUID) returned 404: User not found");
        });
    }

    @Test
    void keepsRawBodyWhenNotExceptionBody() {
        Exception exception = decoder.decode("key", response(502, "Bad Gateway from nginx", Map.of()));

        assertThat(exception.getMessage()).isEqualTo("key returned 502: Bad Gateway from nginx");
    }

    @Test
    void parsesRetryAfterSeconds() {
        Exception exception = decoder.decode("key", response(429, null, Map.of("Retry-After", List.of("7"))));

        assertThat(exception).isInstanceOfSatisfying(UpstreamHttpException.class, upstream -> {
            assertThat(upstream.isRateLimited()).isTrue();
            assertThat(upstream.getRetryAfterSeconds()).isEqualTo(7L);
        });
    }

    @Test
    void ignoresRetryAfterHttpDate() {
        Exception exception = decoder.decode("key",
                response(503, null, Map.of("Retry-After", List.of("Wed, 21 Oct 2026 07:28:00 GMT"))));

        assertThat(((UpstreamHttpException) exception).getRetryAfterSeconds()).isNull();
    }

    private static Response response(int status, String body, Map<String, Collection<String>> headers) {
        Request request = Request.create(Request.HttpMethod.GET, "http://identity-ms/x", Map.of(), null, UTF_8, null);
        Response.Builder builder = Response.builder()
                .status(status)
                .reason("reason")
                .request(request)
                .headers(headers);
        if (body != null) {
            builder.body(body, UTF_8);
        }
        return builder.build();
    }
}
