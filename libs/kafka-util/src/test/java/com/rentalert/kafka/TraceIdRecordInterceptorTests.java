package com.rentalert.kafka;

import com.rentalert.dto.GlobalConstants;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdRecordInterceptorTests {

    private final TraceIdRecordInterceptor interceptor = new TraceIdRecordInterceptor();

    @Test
    void restoresTraceIdFromHeaderForTheListener() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("processing.listings.v1", 0, 5L, "7", "{}");
        record.headers().add(GlobalConstants.TRACE_ID_HEADER, "abc-123".getBytes(StandardCharsets.UTF_8));

        interceptor.intercept(record, null);
        String duringListener = MDC.get(GlobalConstants.TRACE_ID);
        interceptor.afterRecord(record, null);

        assertThat(duringListener).isEqualTo("abc-123");
        assertThat(MDC.get(GlobalConstants.TRACE_ID)).isNull();
    }

    @Test
    void startsNewTraceIdWithoutHeader() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("processing.listings.v1", 0, 5L, "7", "{}");

        assertThat(TraceIdRecordInterceptor.traceIdOf(record)).matches("[0-9a-f-]{36}");
    }
}
