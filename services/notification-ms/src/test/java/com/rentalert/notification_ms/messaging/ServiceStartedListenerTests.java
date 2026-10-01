package com.rentalert.notification_ms.messaging;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.notification_ms.service.AppReadinessService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ServiceStartedListenerTests {

    private final AppReadinessService appReadinessService = mock(AppReadinessService.class);
    private final ServiceStartedListener listener = new ServiceStartedListener(appReadinessService);

    @Test
    void failureIsSwallowedSoNothingGoesToDeadLetter() {
        ServiceStartedEvent event = new ServiceStartedEvent("listing-ms", "host", Instant.now());
        doThrow(new IllegalStateException("redis down")).when(appReadinessService).markStarted(event);

        listener.onServiceStarted(event);

        verify(appReadinessService).markStarted(event);
    }
}
