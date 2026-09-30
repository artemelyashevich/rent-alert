package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotStatusServiceImplTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private BotStatusServiceImpl service;

    @Test
    void marksBotBlockedAndAlertsAdmin() {
        UserProfile profile = profile(BotStatus.ACTIVE);
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(profile));

        service.markBotBlocked(10L);

        assertThat(profile.getSettings().getBotStatus()).isEqualTo(BotStatus.BLOCKED);
        verify(userProfileService).save(profile);
        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.NOTIFICATION_REQUESTS), eq("10"), request.capture());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.BOT_BLOCKED);
        assertThat(request.getValue().templateParams()).containsEntry("message", "10");
    }

    @Test
    void alreadyBlockedIsLeftAlone() {
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(profile(BotStatus.BLOCKED)));

        service.markBotBlocked(10L);

        verify(userProfileService, never()).save(any());
        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void unknownUserIsIgnored() {
        when(userProfileRepository.findById(10L)).thenReturn(Optional.empty());

        service.markBotBlocked(10L);

        verifyNoInteractions(userProfileService, outboxPublisher);
    }

    private static UserProfile profile(BotStatus botStatus) {
        UserProfile profile = UserProfile.builder().build();
        profile.setId(10L);
        profile.getSettings().setBotStatus(botStatus);
        return profile;
    }
}
