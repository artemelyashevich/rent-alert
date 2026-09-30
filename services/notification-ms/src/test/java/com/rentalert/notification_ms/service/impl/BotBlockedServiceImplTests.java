package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BotBlockedServiceImplTests {

    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final BotBlockedServiceImpl service = new BotBlockedServiceImpl(userEventPublisher, notificationService, MINSK);

    @Test
    void reportsBlockedBotAndAlertsAdminOncePerDay() {
        service.handle(42L);

        verify(userEventPublisher).publishBotBlocked(42L);
        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).enqueue(request.capture(), any());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.BOT_BLOCKED);
        assertThat(request.getValue().userId()).isEqualTo(42L);
        assertThat(request.getValue().templateParams()).containsEntry("message", "42");
        assertThat(request.getValue().deduplicationKey()).isEqualTo("bot-blocked:42:" + LocalDate.now(MINSK));
    }
}
