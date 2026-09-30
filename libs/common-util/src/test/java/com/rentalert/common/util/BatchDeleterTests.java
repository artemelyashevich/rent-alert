package com.rentalert.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.cleanup.CleanupResult;
import java.time.Duration;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class BatchDeleterTests {

    @Test
    void deletesInBatchesAndCountsFailedOnes() {
        List<Long> ids = LongStream.rangeClosed(1, 1200).boxed().toList();

        CleanupResult result = BatchDeleter.delete("listings", () -> ids, batch -> {
            if (batch.contains(600L)) {
                throw new IllegalStateException("lock timeout");
            }
            return batch.size();
        });

        assertThat(result.deleted()).isEqualTo(700);
        assertThat(result.failed()).isEqualTo(500);
        assertThat(result.errors()).isEmpty();
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    void reportsSectionThatCouldNotSelectRows() {
        CleanupResult result = BatchDeleter.delete("audit-log", () -> {
            throw new IllegalStateException("connection refused");
        }, batch -> batch.size());

        assertThat(result.deleted()).isZero();
        assertThat(result.errors()).containsExactly("audit-log: connection refused");
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void sumsSections() {
        CleanupResult total = new CleanupResult(3, 1, Duration.ofMillis(10), List.of())
            .plus(new CleanupResult(2, 0, Duration.ofMillis(5), List.of("x: boom")));

        assertThat(total.deleted()).isEqualTo(5);
        assertThat(total.failed()).isEqualTo(1);
        assertThat(total.duration()).hasMillis(15);
        assertThat(total.errors()).containsExactly("x: boom");
    }
}
