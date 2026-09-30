package com.rentalert.common.interceptor;

import com.rentalert.dto.GlobalConstants;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTests {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void keepsIncomingTraceIdForTheWholeRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/listings");
        request.addHeader(GlobalConstants.TRACE_ID_HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringRequest = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> duringRequest.set(MDC.get(GlobalConstants.TRACE_ID)));

        assertThat(duringRequest.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(GlobalConstants.TRACE_ID_HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(GlobalConstants.TRACE_ID)).isNull();
    }

    @Test
    void generatesTraceIdWhenMissingOrMalformed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/listings");
        request.addHeader(GlobalConstants.TRACE_ID_HEADER, "bad value\r\ninjected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(GlobalConstants.TRACE_ID_HEADER))
            .isNotEqualTo("bad value\r\ninjected")
            .matches("[0-9a-f-]{36}");
    }
}
