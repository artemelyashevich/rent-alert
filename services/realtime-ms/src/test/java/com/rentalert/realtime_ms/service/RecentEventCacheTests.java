package com.rentalert.realtime_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecentEventCacheTests {

    private final Clock clock = mock(Clock.class);
    private final RecentEventCache cache = new RecentEventCache(2, Duration.ofMinutes(1), clock);

    @Test
    void addedEventIsContained() {
        UUID eventId = UUID.randomUUID();
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));

        cache.add(eventId);

        assertThat(cache.contains(eventId)).isTrue();
        assertThat(cache.contains(UUID.randomUUID())).isFalse();
    }

    @Test
    void eventExpiresAfterTtl() {
        UUID eventId = UUID.randomUUID();
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));
        cache.add(eventId);

        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T10:01:01Z"));

        assertThat(cache.contains(eventId)).isFalse();
    }

    @Test
    void oldestEventIsEvictedWhenSizeLimitIsExceeded() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));

        cache.add(first);
        cache.add(second);
        cache.add(third);

        assertThat(cache.contains(first)).isFalse();
        assertThat(cache.contains(second)).isTrue();
        assertThat(cache.contains(third)).isTrue();
    }
}
