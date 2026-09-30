package com.rentalert.internal.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.common.security.InternalAuth;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

class InternalTokenInterceptorTests {

    @Test
    void addsServiceToken() {
        RequestTemplate template = new RequestTemplate();

        new InternalTokenInterceptor("secret").apply(template);

        assertThat(template.headers().get(InternalAuth.HEADER)).containsExactly("secret");
    }

    @Test
    void addsNothingWithoutConfiguredToken() {
        RequestTemplate template = new RequestTemplate();

        new InternalTokenInterceptor("").apply(template);

        assertThat(template.headers()).doesNotContainKey(InternalAuth.HEADER);
    }
}
