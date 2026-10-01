package com.rentalert.admin_ms.job;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheCleanupJob {

    private static final int BATCH_SIZE = 1000;

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public long run() {
        Instant started = Instant.now();
        JobExecutionEvent.JobExecutionEventBuilder report = JobExecutionEvent.builder()
            .jobName(JobName.CACHE_CLEAN)
            .initiator(JobInitiator.ADMIN)
            .executionScope(JobScope.CACHE)
            .description("Cache clean up")
            .affectedRecords(0L)
            .startedAt(started);
        try {
            long deleted = deleteAllKeys();
            report.affectedRecords(deleted).status(JobStatus.SUCCESS);
            log.info("Cache cleaned, {} keys deleted", deleted);
            return deleted;
        } catch (RuntimeException e) {
            report.status(JobStatus.FAILED).errorMessage(e.getMessage());
            throw e;
        } finally {
            Instant finished = Instant.now();
            JobExecutionEvent event = report.finishedAt(finished)
                .executionTimeMs(Duration.between(started, finished).toMillis())
                .build();
            kafkaTemplate.send(KafkaTopics.JOB_EVENTS, JobName.CACHE_CLEAN.name(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to report cache cleanup to ops: {}", error.getMessage(), error);
                    }
                });
        }
    }

    private long deleteAllKeys() {
        long deleted = 0;
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match("*").count(BATCH_SIZE).build())) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= BATCH_SIZE) {
                    deleted += delete(batch);
                }
            }
        }
        if (!batch.isEmpty()) {
            deleted += delete(batch);
        }
        return deleted;
    }

    private long delete(List<String> batch) {
        Long removed = redisTemplate.delete(batch);
        batch.clear();
        return removed == null ? 0 : removed;
    }
}
