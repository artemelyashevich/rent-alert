package com.rentalert.common.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class OutgoingRequestLoggingInterceptorTests {

    @Test
    void hidesBotTokenAndQuery() {
        URI uri = URI.create("https://api.telegram.org/bot123456:ABC-secret/sendMessage?chat_id=42");

        assertThat(OutgoingRequestLoggingInterceptor.describe(uri)).isEqualTo("api.telegram.org/bot***/sendMessage");
    }

    @Test
    void keepsRegularPath() {
        URI uri = URI.create("https://api.kufar.by/search-api/v2/search/rendered-paginated?cat=1010&size=100");

        assertThat(OutgoingRequestLoggingInterceptor.describe(uri))
            .isEqualTo("api.kufar.by/search-api/v2/search/rendered-paginated");
    }
}
