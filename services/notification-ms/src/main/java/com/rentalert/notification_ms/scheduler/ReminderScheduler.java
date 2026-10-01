package com.rentalert.notification_ms.scheduler;

import com.rentalert.notification_ms.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(fixedDelayString = "${app.notification.reminder-interval:PT30S}")
    public void sendDue() {
        reminderService.send();
    }
}
