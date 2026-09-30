package com.rentalert.kafka;

import com.rentalert.dto.GlobalConstants;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

@Slf4j
public class TraceIdRecordInterceptor implements RecordInterceptor<String, String> {

    @Override
    public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record,
                                                    Consumer<String, String> consumer) {
        MDC.put(GlobalConstants.TRACE_ID, traceIdOf(record));
        log.debug("[KAFKA] {}-{}@{} key={}", record.topic(), record.partition(), record.offset(), record.key());
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
        MDC.remove(GlobalConstants.TRACE_ID);
    }

    static String traceIdOf(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(GlobalConstants.TRACE_ID_HEADER);
        return header == null || header.value() == null
            ? UUID.randomUUID().toString()
            : new String(header.value(), StandardCharsets.UTF_8);
    }
}
