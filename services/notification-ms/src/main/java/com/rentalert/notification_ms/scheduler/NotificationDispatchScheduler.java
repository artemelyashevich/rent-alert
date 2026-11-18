package com.rentalert.notification_ms.scheduler;

import com.rentalert.common.service.LockService;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchScheduler {

    private static final Duration LOCK_LEASE = Duration.ofMinutes(5);

    private final LockService lockService;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.notification.dispatch-interval:PT2S}")
    public void dispatchDue() {
        if (!lockService.tryWithLock("scheduled:notification-dispatch", LOCK_LEASE, notificationService::retryFailed)) {
            log.debug("Notification dispatch skipped, another instance owns the lock");
        }
    }
}
