package com.rentalert.roommate_ms.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.roommate_ms.repository.RoommateRetentionCleanupRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class RoommateCleanupParticipantTests {

    private static final Instant REFERENCE_TIME = Instant.parse("2026-09-30T21:00:00Z");
    private static final Duration RETENTION = Duration.ofDays(180);
    private static final Instant CUTOFF = REFERENCE_TIME.minus(RETENTION);

    private final RoommateRetentionCleanupRepository repository = mock(RoommateRetentionCleanupRepository.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final RoommateCleanupParticipant participant =
        new RoommateCleanupParticipant("roommate-ms", 20000, RETENTION, repository, transactionTemplate);

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    void deletesStaleRowsOfEverySectionWithCutoffFromReferenceTime() {
        when(repository.findStaleMatchEventIds(CUTOFF, 20000)).thenReturn(List.of(1L, 2L));
        when(repository.deleteMatchEventsByIdIn(List.of(1L, 2L))).thenReturn(2);
        when(repository.findStaleViewingIds(CUTOFF, 20000)).thenReturn(List.of(7L));
        when(repository.deleteViewingsByIdIn(List.of(7L))).thenReturn(1);
        when(repository.findResolvedModerationTaskIds(CUTOFF, 20000)).thenReturn(List.of(3L, 4L, 5L));
        when(repository.deleteModerationTasksByIdIn(List.of(3L, 4L, 5L))).thenReturn(3);

        CleanupResult result = participant.cleanUp(REFERENCE_TIME);

        assertThat(result.deleted()).isEqualTo(6);
        assertThat(result.failed()).isZero();
        assertThat(result.hasFailures()).isFalse();
        verify(repository).deleteMatchEventsByIdIn(List.of(1L, 2L));
        verify(repository).deleteViewingsByIdIn(List.of(7L));
        verify(repository).deleteModerationTasksByIdIn(List.of(3L, 4L, 5L));
    }

    @Test
    void doesNothingWhenNoStaleRows() {
        when(repository.findStaleMatchEventIds(eq(CUTOFF), eq(20000))).thenReturn(List.of());
        when(repository.findStaleViewingIds(eq(CUTOFF), eq(20000))).thenReturn(List.of());
        when(repository.findResolvedModerationTaskIds(eq(CUTOFF), eq(20000))).thenReturn(List.of());

        CleanupResult result = participant.cleanUp(REFERENCE_TIME);

        assertThat(result.deleted()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void reportsFailedSelectionOfOneSectionAndKeepsCleaningOthers() {
        when(repository.findStaleMatchEventIds(any(), anyInt())).thenReturn(List.of());
        when(repository.findStaleViewingIds(any(), anyInt())).thenThrow(new IllegalStateException("connection refused"));
        when(repository.findResolvedModerationTaskIds(CUTOFF, 20000)).thenReturn(List.of(9L));
        when(repository.deleteModerationTasksByIdIn(List.of(9L))).thenReturn(1);

        CleanupResult result = participant.cleanUp(REFERENCE_TIME);

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(result.errors()).containsExactly("roommate-viewings: connection refused");
        assertThat(result.hasFailures()).isTrue();
    }
}
