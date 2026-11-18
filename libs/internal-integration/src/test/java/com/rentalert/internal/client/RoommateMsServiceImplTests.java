package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.dto.roommate.RoommateMatchStatus;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.dto.roommate.RoommateViewingDto;
import com.rentalert.dto.roommate.RoommateViewingStatus;
import com.rentalert.internal.client.service.RoommateMsService;
import com.rentalert.internal.client.service.impl.RoommateMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class RoommateMsServiceImplTests {

    private static final UpstreamHttpException UNAVAILABLE = new UpstreamHttpException("unavailable", 503, null);

    private final RoommateMsClient client = mock(RoommateMsClient.class);
    private final RoommateMsService service = new RoommateMsServiceImpl(
        client,
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build()),
        CircuitBreakerRegistry.ofDefaults()
    );

    @Test
    void queueIsRetriedWithPagingEncoded() {
        PageResponse<RoommateModerationTaskResponse> empty = new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(client.findModerationQueue(0, 20, List.of("createdAt,ASC")))
            .thenThrow(UNAVAILABLE)
            .thenReturn(empty);

        assertThat(service.findModerationQueue(PageRequest.of(0, 20, Sort.by("createdAt")))).isEqualTo(empty);
        verify(client, times(2)).findModerationQueue(0, 20, List.of("createdAt,ASC"));
    }

    @Test
    void decisionsAreNotRetried() {
        RoommateModerationDecisionRequest request = new RoommateModerationDecisionRequest("spam");
        when(client.approveModeration(any(), any())).thenThrow(UNAVAILABLE);
        when(client.rejectModeration(any(), any())).thenThrow(UNAVAILABLE);

        assertThatThrownBy(() -> service.approveModeration(7L, request)).isSameAs(UNAVAILABLE);
        assertThatThrownBy(() -> service.rejectModeration(7L, request)).isSameAs(UNAVAILABLE);
        verify(client, times(1)).approveModeration(7L, request);
        verify(client, times(1)).rejectModeration(7L, request);
    }

    @Test
    void viewingIsRetriedAndMissingViewingIsEmpty() {
        RoommateViewingDto viewing = new RoommateViewingDto(5L, 10L, 1L, 2L, Instant.parse("2026-10-05T12:00:00Z"),
            RoommateViewingStatus.SCHEDULED, 15L, "Немига 5", "2-комн.", "Немига 5");
        when(client.getViewing(5L)).thenThrow(UNAVAILABLE).thenReturn(viewing);
        when(client.getViewing(6L)).thenReturn(null);

        assertThat(service.getViewing(5L)).contains(viewing);
        assertThat(service.getViewing(6L)).isEmpty();
        verify(client, times(2)).getViewing(5L);
    }

    @Test
    void matchParticipantsAreRetriedAndEmptyBodyIsEmptyList() {
        RoommateMatchParticipantsDto participants = new RoommateMatchParticipantsDto(10L,
            RoommateMatchStatus.MATCHED, 1L, 2L, Instant.parse("2026-10-01T10:00:00Z"));
        when(client.findMatchParticipants(List.of(10L, 11L))).thenThrow(UNAVAILABLE).thenReturn(List.of(participants));
        when(client.findMatchParticipants(List.of(12L))).thenReturn(null);

        assertThat(service.findMatchParticipants(List.of(10L, 11L))).containsExactly(participants);
        assertThat(service.findMatchParticipants(List.of(12L))).isEmpty();
        verify(client, times(2)).findMatchParticipants(List.of(10L, 11L));
    }
}
