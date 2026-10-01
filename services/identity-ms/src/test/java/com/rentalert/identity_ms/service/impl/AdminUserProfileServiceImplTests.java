package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import com.rentalert.redis.service.UserSessionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AdminUserProfileServiceImplTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private AdminUserProfileServiceImpl service;

    @BeforeEach
    void startTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void deletePublishesUserDeletedAndDropsSessionAfterCommit() {
        when(userProfileRepository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(userProfileRepository).deleteById(5L);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq("5"), event.capture());
        UserActionEvent deleted = (UserActionEvent) event.getValue();
        assertThat(deleted.getEvent()).isEqualTo(UserEvent.USER_DELETED);
        assertThat(deleted.getInitiatorId()).isEqualTo(5L);
        verify(userProfileService).evictCachedProfile(5L);

        verify(userSessionService, never()).invalidateSession(anyLong());
        commit();
        verify(userSessionService).invalidateSession(5L);
    }

    @Test
    void deletingMissingUserIsNotFound() {
        when(userProfileRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L))
            .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(outboxPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void roleChangeDropsSessionAfterCommit() {
        UserProfile profile = profile(5L, "tg-enc", UserRole.USER, BotStatus.ACTIVE);
        when(userProfileRepository.findById(5L)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        service.updateRole(5L, UserRole.ADMIN);

        assertThat(profile.getSettings().getRoleName()).isEqualTo(UserRole.ADMIN);
        verify(userProfileService).evictCachedProfile(5L);
        commit();
        verify(userSessionService).invalidateSession(5L);
    }

    @Test
    void broadcastToEveryoneTargetsAllNonAdmins() {
        when(userProfileRepository.findAllNonAdmins())
            .thenReturn(List.of(profile(1L, "tg-1", UserRole.USER, BotStatus.ACTIVE), profile(2L, null, UserRole.USER, BotStatus.BLOCKED)));

        assertThat(service.findBroadcastTargets(List.of())).containsExactly(
            new BroadcastTarget(1L, "tg-1", BotStatus.ACTIVE),
            new BroadcastTarget(2L, null, BotStatus.BLOCKED));
        verify(userProfileRepository, never()).findNonAdminsByIdIn(any());
    }

    @Test
    void broadcastToChosenUsersSkipsOthers() {
        when(userProfileRepository.findNonAdminsByIdIn(List.of(1L, 3L)))
            .thenReturn(List.of(profile(1L, "tg-1", UserRole.USER, BotStatus.ACTIVE)));

        assertThat(service.findBroadcastTargets(List.of(1L, 3L))).extracting(BroadcastTarget::userId).containsExactly(1L);
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private static UserProfile profile(Long id, String telegramId, UserRole role, BotStatus botStatus) {
        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setTelegramId(telegramId);
        profile.setSettings(UserSettings.builder().roleName(role).botStatus(botStatus).build());
        return profile;
    }
}
