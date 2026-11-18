package com.rentalert.realtime_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.RealtimePushEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.realtime_ms.service.RecentEventCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimePushListener {

    static final String USER_DESTINATION = "/queue/notifications";

    private final RecentEventCache recentEventCache;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaTopics.REALTIME_PUSH, groupId = "realtime-ms-${random.uuid}")
    public void onRealtimePush(RealtimePushEvent event) {
        if (event == null || event.eventId() == null || event.userId() == null || event.userId().isBlank()
            || event.payload() == null) {
            throw new NonRetryableEventException("Realtime push event without eventId, userId or payload");
        }
        if (recentEventCache.contains(event.eventId())) {
            log.debug("Duplicate realtime push skipped. eventId={}", event.eventId());
            return;
        }
        messagingTemplate.convertAndSendToUser(event.userId(), USER_DESTINATION, event.payload());
        recentEventCache.add(event.eventId());
        log.debug("Realtime push delivered. eventId={}, userId={}", event.eventId(), event.userId());
    }
}
