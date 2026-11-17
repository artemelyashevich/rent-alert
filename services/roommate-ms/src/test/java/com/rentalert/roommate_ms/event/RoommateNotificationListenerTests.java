package com.rentalert.roommate_ms.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateViewingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoommateNotificationListenerTests {

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final RoommateNotificationPublisher notificationPublisher = mock(RoommateNotificationPublisher.class);
    private final RoommateViewingRepository viewingRepository = mock(RoommateViewingRepository.class);

    private final RoommateNotificationListener listener =
        new RoommateNotificationListener(identityMsService, notificationPublisher, viewingRepository);

    private void briefsFor(Long... userIds) {
        when(identityMsService.findUserBriefs(any())).thenReturn(
            List.of(userIds).stream().map(id -> new UserBriefResponse(id, "user" + id, null)).toList());
    }

    @Test
    void newInterest_notifiesCounterpartyOnlyWithActorNameAndRealtimeParams() {
        briefsFor(1L, 2L);

        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, null, RoommateMatchStatus.WAITING_RESPONSE,
            1L, List.of(1L, 2L), 2L, null));

        verify(notificationPublisher).publish(eq(2L), eq(NotificationType.ROOMMATE_NEW_INTEREST),
            argThat(params -> params.get("message").endsWith("Собеседник: user1.")
                && "100".equals(params.get("matchId"))
                && "WAITING_RESPONSE".equals(params.get("toStatus"))
                && !params.containsKey("fromStatus")),
            eq("ROOMMATE:ROOMMATE_NEW_INTEREST:100:2:WAITING_RESPONSE"), isNull());
        verify(notificationPublisher, times(1)).publish(any(), any(), anyMap(), anyString(), any());
    }

    @Test
    void matched_notifiesBothParticipantsEachWithTheOtherName() {
        briefsFor(1L, 2L);

        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.WAITING_RESPONSE, RoommateMatchStatus.MATCHED,
            1L, List.of(1L, 2L), 2L, null));

        verify(notificationPublisher).publish(eq(1L), eq(NotificationType.ROOMMATE_MATCHED),
            argThat(params -> params.get("message").endsWith("Собеседник: user2.")), anyString(), isNull());
        verify(notificationPublisher).publish(eq(2L), eq(NotificationType.ROOMMATE_MATCHED),
            argThat(params -> params.get("message").endsWith("Собеседник: user1.")), anyString(), isNull());
    }

    @Test
    void declineBySystem_isNotNotified() {
        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.CHATTING, RoommateMatchStatus.DECLINED,
            null, List.of(1L, 2L), 2L, null));

        verifyNoInteractions(notificationPublisher);
    }

    @Test
    void expired_notifiesInitiatorWithoutNameWhenIdentityHasNone() {
        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.WAITING_RESPONSE, RoommateMatchStatus.EXPIRED,
            null, List.of(1L, 2L), 1L, null));

        verify(notificationPublisher).publish(eq(1L), eq(NotificationType.ROOMMATE_INTEREST_EXPIRED),
            eq(Map.of("message", "Ваш интерес в разделе «Соседи» истёк без ответа.",
                "matchId", "100", "fromStatus", "WAITING_RESPONSE", "toStatus", "EXPIRED", "scenario", "PARTNER")),
            eq("ROOMMATE:ROOMMATE_INTEREST_EXPIRED:100:1:EXPIRED"), isNull());
    }

    @Test
    void unmappedTransition_cancelViewingBack_sendsNothingAndLooksUpNoNames() {
        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.VIEWING_TOGETHER, RoommateMatchStatus.CHATTING,
            1L, List.of(1L, 2L), 2L, null));

        verifyNoInteractions(notificationPublisher, identityMsService);
    }

    @Test
    void identityFailure_sendsWithoutName() {
        when(identityMsService.findUserBriefs(any())).thenThrow(new IllegalStateException("identity down"));

        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, null, RoommateMatchStatus.WAITING_RESPONSE,
            1L, List.of(1L, 2L), 2L, null));

        verify(notificationPublisher).publish(eq(2L), eq(NotificationType.ROOMMATE_NEW_INTEREST),
            argThat(params -> params.get("message").equals("Кто-то заинтересовался вами в разделе «Соседи».")),
            anyString(), isNull());
    }

    @Test
    void viewingScheduled_farEnoughInFuture_notifiesCounterpartyAndSchedulesRemindersForBoth() {
        briefsFor(1L, 2L);
        Instant viewingDate = Instant.now().plus(Duration.ofDays(3));
        RoommateViewing viewing = RoommateViewing.builder()
            .id(555L)
            .viewingDate(viewingDate)
            .address("ул. Тестовая, 1")
            .status(RoommateViewingStatus.SCHEDULED)
            .build();
        when(viewingRepository.findById(555L)).thenReturn(Optional.of(viewing));

        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.CHATTING, RoommateMatchStatus.VIEWING_TOGETHER,
            1L, List.of(1L, 2L), 2L, 555L));

        verify(notificationPublisher).publish(eq(2L), eq(NotificationType.ROOMMATE_VIEWING_SCHEDULED),
            anyMap(), anyString(), isNull());
        verify(notificationPublisher).publish(eq(1L), eq(NotificationType.ROOMMATE_VIEWING_REMINDER),
            eq(Map.of("message", "Завтра у вас совместный просмотр: ул. Тестовая, 1")),
            eq("ROOMMATE_VIEWING_REMINDER:555:1"), eq(viewingDate.minus(Duration.ofDays(1))));
        verify(notificationPublisher).publish(eq(2L), eq(NotificationType.ROOMMATE_VIEWING_REMINDER),
            anyMap(), eq("ROOMMATE_VIEWING_REMINDER:555:2"), eq(viewingDate.minus(Duration.ofDays(1))));
    }

    @Test
    void viewingScheduled_lessThanADayAway_skipsReminder() {
        briefsFor(1L, 2L);
        RoommateViewing viewing = RoommateViewing.builder()
            .id(555L)
            .viewingDate(Instant.now().plus(Duration.ofHours(3)))
            .status(RoommateViewingStatus.SCHEDULED)
            .build();
        when(viewingRepository.findById(555L)).thenReturn(Optional.of(viewing));

        listener.onTransition(new RoommateMatchTransitionEvent(
            100L, RoommateScenario.PARTNER, RoommateMatchStatus.CHATTING, RoommateMatchStatus.VIEWING_TOGETHER,
            1L, List.of(1L, 2L), 2L, 555L));

        verify(notificationPublisher, times(1)).publish(any(), eq(NotificationType.ROOMMATE_VIEWING_SCHEDULED),
            anyMap(), anyString(), isNull());
        verify(notificationPublisher, never()).publish(any(), eq(NotificationType.ROOMMATE_VIEWING_REMINDER),
            anyMap(), anyString(), any());
    }
}
