package com.rentalert.notification_ms.messaging;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.notification_ms.service.AppReadinessService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ServiceStartedListenerTests {

    private final AppReadinessService appReadinessService = mock(AppReadinessService.class);
    private final ServiceStartedListener listener = createListener();

    private ServiceStartedListener createListener() {
        ServiceStartedListener created = new ServiceStartedListener(appReadinessService);
        ReflectionTestUtils.setField(created, "inboxGuard", new InMemoryInboxGuard());
        return created;
    }

    @Test
    void failureIsSwallowedSoNothingGoesToDeadLetter() {
        ServiceStartedEvent event = new ServiceStartedEvent("listing-ms", "host", Instant.now());
        doThrow(new IllegalStateException("redis down")).when(appReadinessService).markStarted(event);

        listener.onServiceStarted(event);

        verify(appReadinessService).markStarted(event);
    }

    @Test
    void redeliveredStartIsRecordedOnce() {
        Instant startedAt = Instant.parse("2026-11-01T10:00:00Z");

        listener.onServiceStarted(new ServiceStartedEvent("listing-ms", "host", startedAt));
        listener.onServiceStarted(new ServiceStartedEvent("listing-ms", "host", startedAt));

        verify(appReadinessService, times(1)).markStarted(new ServiceStartedEvent("listing-ms", "host", startedAt));
    }
}
