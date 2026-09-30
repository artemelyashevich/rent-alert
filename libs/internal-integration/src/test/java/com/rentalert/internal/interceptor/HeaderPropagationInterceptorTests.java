package com.rentalert.internal.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.GlobalConstants;
import com.rentalert.internal.annotation.IgnoreInterceptRequest;
import feign.Contract;
import feign.MethodMetadata;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HeaderPropagationInterceptorTests {

    private final HeaderPropagationInterceptor interceptor = new HeaderPropagationInterceptor();

    private static RequestTemplate template(Class<?> type, String methodName) {
        MethodMetadata metadata = new Contract.Default().parseAndValidateMetadata(type).stream()
            .filter(md -> md.method().getName().equals(methodName))
            .findFirst()
            .orElseThrow();
        RequestTemplate template = new RequestTemplate();
        template.methodMetadata(metadata);
        template.feignTarget(new Target.HardCodedTarget<>(type, "http://identity-ms"));
        return template;
    }

    @BeforeEach
    void bindRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void propagatesHeaders() {
        RequestTemplate template = template(PlainClient.class, "propagated");

        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token");
        assertThat(template.headers().get(HttpHeaders.ACCEPT_LANGUAGE)).containsExactly("en");
    }

    @Test
    void propagatesTraceIdFromMdcEvenWithoutRequest() {
        RequestContextHolder.resetRequestAttributes();
        MDC.put(GlobalConstants.TRACE_ID, "abc-123");
        try {
            RequestTemplate template = template(PlainClient.class, "propagated");

            interceptor.apply(template);

            assertThat(template.headers().get(GlobalConstants.TRACE_ID_HEADER)).containsExactly("abc-123");
        } finally {
            MDC.remove(GlobalConstants.TRACE_ID);
        }
    }

    @Test
    void emptyIgnoreOnMethodSkipsAllHeaders() {
        RequestTemplate template = template(PlainClient.class, "ignoreAll");

        interceptor.apply(template);

        assertThat(template.headers()).isEmpty();
    }

    @Test
    void ignoreWithHeadersSkipsOnlyListedCaseInsensitive() {
        RequestTemplate template = template(PlainClient.class, "ignoreAuth");

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
        assertThat(template.headers().get(HttpHeaders.ACCEPT_LANGUAGE)).containsExactly("en");
    }

    @Test
    void ignoreOnClientTypeSkipsHeaders() {
        RequestTemplate template = template(IgnoredClient.class, "call");

        interceptor.apply(template);

        assertThat(template.headers()).isEmpty();
    }

    @Test
    void keepsHeaderSetByClient() {
        RequestTemplate template = template(PlainClient.class, "propagated");
        template.header(HttpHeaders.AUTHORIZATION, "Bearer service");

        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer service");
    }

    @Test
    void doesNothingOutsideHttpRequest() {
        RequestContextHolder.resetRequestAttributes();
        RequestTemplate template = template(PlainClient.class, "propagated");

        interceptor.apply(template);

        assertThat(template.headers()).isEmpty();
    }

    interface PlainClient {

        @RequestLine("GET /propagated")
        void propagated();

        @IgnoreInterceptRequest
        @RequestLine("GET /ignoreAll")
        void ignoreAll();

        @IgnoreInterceptRequest(headers = "authorization")
        @RequestLine("GET /ignoreAuth")
        void ignoreAuth();
    }

    @IgnoreInterceptRequest
    interface IgnoredClient {

        @RequestLine("GET /x")
        void call();
    }
}
