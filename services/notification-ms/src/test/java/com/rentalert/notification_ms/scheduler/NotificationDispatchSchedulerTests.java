package com.rentalert.notification_ms.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.service.LockService;
import com.rentalert.notification_ms.service.NotificationService;
import org.junit.jupiter.api.Test;

class NotificationDispatchSchedulerTests {

    private final LockService lockService = mock(LockService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationDispatchScheduler scheduler = new NotificationDispatchScheduler(lockService, notificationService);

    @Test
    void retriesFailedUnderLock() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(lockService).tryWithLock(eq("scheduled:notification-dispatch"), any(), any(Runnable.class));

        scheduler.dispatchDue();

        verify(notificationService).retryFailed();
    }

    @Test
    void skipsRetryWhenAnotherInstanceHoldsLock() {
        when(lockService.tryWithLock(eq("scheduled:notification-dispatch"), any(), any(Runnable.class))).thenReturn(false);

        scheduler.dispatchDue();

        verify(notificationService, never()).retryFailed();
    }
}
