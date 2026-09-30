package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.dto.event.user.UserEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Events produced by other services must survive the JSON round trip of the outbox and the listener. */
class OpsEventSerializationTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void userActionEventRoundTrip() {
        UserActionEvent event = UserActionEvent.builder()
            .event(UserEvent.HIDDEN_LISTING)
            .params(List.of("15"))
            .build();
        event.setInitiatorId(42L);

        UserActionEvent read = jsonMapper.readValue(jsonMapper.writeValueAsString(event), UserActionEvent.class);

        assertThat(read.getIdempotencyKey()).isEqualTo(event.getIdempotencyKey());
        assertThat(read.getEvent()).isEqualTo(UserEvent.HIDDEN_LISTING);
        assertThat(read.getParams()).containsExactly("15");
        assertThat(read.getInitiatorId()).isEqualTo(42L);
        assertThat(read.getEventType()).isEqualTo(EventInitiatorType.USER);
    }

    @Test
    void jobExecutionEventRoundTrip() {
        Instant started = Instant.parse("2026-09-29T10:00:00Z");
        JobExecutionEvent event = JobExecutionEvent.builder()
            .jobName(JobName.CURRENCY_RATE_SYNC)
            .initiator(JobInitiator.INTERNAL)
            .executionScope(JobScope.CACHE_DB_SYNC)
            .affectedRecords(6L)
            .startedAt(started)
            .finishedAt(started.plusSeconds(2))
            .executionTimeMs(2000L)
            .status(JobStatus.SUCCESS)
            .build();

        JobExecutionEvent read = jsonMapper.readValue(jsonMapper.writeValueAsString(event), JobExecutionEvent.class);

        assertThat(read.getIdempotencyKey()).isEqualTo(event.getIdempotencyKey());
        assertThat(read.getJobName()).isEqualTo(JobName.CURRENCY_RATE_SYNC);
        assertThat(read.getAffectedRecords()).isEqualTo(6L);
        assertThat(read.getStartedAt()).isEqualTo(started);
        assertThat(read.getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(read.getEventType()).isEqualTo(EventInitiatorType.INTERNAL);
    }
}
