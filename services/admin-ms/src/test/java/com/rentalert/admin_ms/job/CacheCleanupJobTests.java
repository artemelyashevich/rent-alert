package com.rentalert.admin_ms.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobStatus;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

class CacheCleanupJobTests {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final CacheCleanupJob job = new CacheCleanupJob(redisTemplate, kafkaTemplate);

    @Test
    @SuppressWarnings("unchecked")
    void deletesAllKeysAndReportsJobToOps() {
        Iterator<String> keys = List.of("session:a", "cache:b").iterator();
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenAnswer(invocation -> keys.hasNext());
        when(cursor.next()).thenAnswer(invocation -> keys.next());
        when(redisTemplate.scan(any())).thenReturn(cursor);
        when(redisTemplate.delete(List.of("session:a", "cache:b"))).thenReturn(2L);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(new CompletableFuture<>());

        assertThat(job.run()).isEqualTo(2L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.JOB_EVENTS), eq(JobName.CACHE_CLEAN.name()), event.capture());
        JobExecutionEvent report = (JobExecutionEvent) event.getValue();
        assertThat(report.getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(report.getInitiator()).isEqualTo(JobInitiator.ADMIN);
        assertThat(report.getAffectedRecords()).isEqualTo(2L);
    }
}
