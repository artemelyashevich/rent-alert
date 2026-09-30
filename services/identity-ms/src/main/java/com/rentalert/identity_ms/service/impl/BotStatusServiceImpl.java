package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.BotStatusService;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotStatusServiceImpl implements BotStatusService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public void markBotBlocked(Long userId) {
        Optional<UserProfile> found = userProfileRepository.findById(userId);
        if (found.isEmpty()) {
            log.warn("Bot blocked by unknown user {}, ignoring", userId);
            return;
        }
        UserProfile profile = found.get();
        if (profile.getSettings().getBotStatus() == BotStatus.BLOCKED) {
            return;
        }
        profile.getSettings().setBotStatus(BotStatus.BLOCKED);
        userProfileService.save(profile);
        outboxPublisher.publish(KafkaTopics.notificationRequests(NotificationType.BOT_BLOCKED), String.valueOf(userId),
            NotificationRequest.builder()
                .producer("identity-ms")
                .userId(userId)
                .type(NotificationType.BOT_BLOCKED)
                .templateParams(Map.of("message", String.valueOf(userId)))
                .build());
        log.warn("Bot was blocked by user {}", userId);
    }
}
