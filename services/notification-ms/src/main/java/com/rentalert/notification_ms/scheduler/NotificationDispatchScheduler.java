package com.rentalert.notification_ms.scheduler;

import com.rentalert.notification_ms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDispatchScheduler {

    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.notification.dispatch-interval:PT2S}")
    public void dispatchDue() {
        notificationService.retryFailed();
    }
}
