package com.rentalert.roommate_ms.event;

import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateViewingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns a roommate match transition into notifications, queued in the outbox of the transaction that made the
 * transition, so a rolled-back transition sends nothing. Counterparty names come from identity-ms and are optional:
 * when identity is unavailable the notification is sent without a name.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoommateNotificationListener {

    private static final Duration VIEWING_REMINDER_LEAD = Duration.ofDays(1);

    private final IdentityMsService identityMsService;
    private final RoommateNotificationPublisher notificationPublisher;
    private final RoommateViewingRepository viewingRepository;

    /**
     * Runs before the commit of the transition, so the outbox records join its transaction. Transitions without a
     * notification and viewing reminders for matches less than a day away are skipped.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTransition(RoommateMatchTransitionEvent event) {
        NotificationType type = resolveType(event);
        if (type != null) {
            Map<Long, String> names = findNames(event.participantUserIds());
            for (Long recipientId : resolveRecipients(event, type)) {
                Long counterpartyId = counterpartyOf(event, recipientId);
                String name = counterpartyId == null ? null : names.get(counterpartyId);
                String message = buildMessage(type, name);
                notificationPublisher.publish(recipientId, type, realtimeParams(event, message),
                    dedupKey(event, type, recipientId), null);
            }
        }

        if (event.toStatus() == RoommateMatchStatus.VIEWING_TOGETHER && event.roommateViewingId() != null) {
            scheduleViewingReminders(event);
        }
    }

    private NotificationType resolveType(RoommateMatchTransitionEvent e) {
        if (e.fromStatus() == null && e.toStatus() == RoommateMatchStatus.WAITING_RESPONSE) {
            return NotificationType.ROOMMATE_NEW_INTEREST;
        }
        if (e.fromStatus() == RoommateMatchStatus.WAITING_RESPONSE && e.toStatus() == RoommateMatchStatus.MATCHED) {
            return NotificationType.ROOMMATE_MATCHED;
        }
        if (e.fromStatus() == RoommateMatchStatus.MATCHED && e.toStatus() == RoommateMatchStatus.CHATTING) {
            return NotificationType.ROOMMATE_CHAT_STARTED;
        }
        if (e.fromStatus() == RoommateMatchStatus.CHATTING && e.toStatus() == RoommateMatchStatus.VIEWING_TOGETHER) {
            return NotificationType.ROOMMATE_VIEWING_SCHEDULED;
        }
        if (e.toStatus() == RoommateMatchStatus.DECLINED && e.actorUserId() != null) {
            return NotificationType.ROOMMATE_MATCH_ENDED;
        }
        if (e.fromStatus() == RoommateMatchStatus.WAITING_RESPONSE && e.toStatus() == RoommateMatchStatus.EXPIRED) {
            return NotificationType.ROOMMATE_INTEREST_EXPIRED;
        }
        return null;
    }

    private List<Long> resolveRecipients(RoommateMatchTransitionEvent e, NotificationType type) {
        if (type == NotificationType.ROOMMATE_MATCHED) {
            return e.participantUserIds();
        }
        return e.counterpartyUserId() == null ? List.of() : List.of(e.counterpartyUserId());
    }

    private static Long counterpartyOf(RoommateMatchTransitionEvent event, Long recipientId) {
        return event.participantUserIds().stream()
            .filter(id -> !Objects.equals(id, recipientId))
            .findFirst()
            .orElse(null);
    }

    private Map<Long, String> findNames(List<Long> userIds) {
        try {
            return identityMsService.findUserBriefs(userIds).stream()
                .filter(brief -> brief.userId() != null && brief.username() != null)
                .collect(Collectors.toMap(UserBriefResponse::userId, UserBriefResponse::username, (a, b) -> a));
        } catch (RuntimeException e) {
            log.warn("Roommate notifications are sent without counterparty names: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String buildMessage(NotificationType type, String counterpartyName) {
        String base = switch (type) {
            case ROOMMATE_NEW_INTEREST -> "Кто-то заинтересовался вами в разделе «Соседи».";
            case ROOMMATE_MATCHED -> "Взаимный интерес! Вы понравились друг другу в разделе «Соседи» — можно начинать общение.";
            case ROOMMATE_CHAT_STARTED -> "Собеседник начал общение в разделе «Соседи».";
            case ROOMMATE_VIEWING_SCHEDULED -> "Назначен совместный просмотр — загляните в календарь.";
            case ROOMMATE_MATCH_ENDED -> "Матч в разделе «Соседи» завершён.";
            case ROOMMATE_INTEREST_EXPIRED -> "Ваш интерес в разделе «Соседи» истёк без ответа.";
            default -> throw new IllegalStateException("No message template for " + type);
        };
        return counterpartyName == null ? base : base + " Собеседник: " + counterpartyName + ".";
    }

    private static Map<String, String> realtimeParams(RoommateMatchTransitionEvent event, String message) {
        Map<String, String> params = new HashMap<>();
        params.put("message", message);
        params.put("matchId", String.valueOf(event.matchId()));
        if (event.scenario() != null) {
            params.put("scenario", event.scenario().name());
        }
        if (event.fromStatus() != null) {
            params.put("fromStatus", event.fromStatus().name());
        }
        if (event.toStatus() != null) {
            params.put("toStatus", event.toStatus().name());
        }
        return params;
    }

    private void scheduleViewingReminders(RoommateMatchTransitionEvent event) {
        RoommateViewing viewing = viewingRepository.findById(event.roommateViewingId()).orElse(null);
        if (viewing == null || viewing.getViewingDate() == null) {
            log.warn("Roommate viewing not found for reminder scheduling. viewingId={}", event.roommateViewingId());
            return;
        }
        Instant remindAt = viewing.getViewingDate().minus(VIEWING_REMINDER_LEAD);
        if (remindAt.isBefore(Instant.now())) {
            return;
        }
        String message = "Завтра у вас совместный просмотр"
            + (viewing.getAddress() == null ? "." : ": " + viewing.getAddress());

        for (Long userId : event.participantUserIds()) {
            notificationPublisher.publish(userId, NotificationType.ROOMMATE_VIEWING_REMINDER,
                Map.of("message", message), "ROOMMATE_VIEWING_REMINDER:" + viewing.getId() + ":" + userId, remindAt);
        }
    }

    private static String dedupKey(RoommateMatchTransitionEvent event, NotificationType type, Long recipientId) {
        return "ROOMMATE:" + type + ":" + event.matchId() + ":" + recipientId + ":"
            + (event.toStatus() == null ? "" : event.toStatus().name());
    }
}
