package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.notification.BroadcastRecipientStatus;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationDispatchResult;
import com.rentalert.dto.notification.NotificationDispatchStatus;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.notification_ms.domain.NotificationBroadcast;
import com.rentalert.notification_ms.domain.NotificationBroadcastRecipient;
import com.rentalert.notification_ms.mapper.NotificationBroadcastMapper;
import com.rentalert.notification_ms.mapper.NotificationBroadcastRecipientMapper;
import com.rentalert.notification_ms.repository.NotificationBroadcastRecipientRepository;
import com.rentalert.notification_ms.repository.NotificationBroadcastRepository;
import com.rentalert.notification_ms.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class AdminBroadcastServiceImplTests {

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final NotificationBroadcastRepository broadcastRepository = mock(NotificationBroadcastRepository.class);
    private final NotificationBroadcastRecipientRepository recipientRepository = mock(NotificationBroadcastRecipientRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final CryptoService cryptoService = mock(CryptoService.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final AdminBroadcastServiceImpl service = new AdminBroadcastServiceImpl(identityMsService, broadcastRepository,
        recipientRepository, mock(NotificationBroadcastMapper.class), mock(NotificationBroadcastRecipientMapper.class),
        notificationService, cryptoService, transactionTemplate);

    private final NotificationBroadcast broadcast = NotificationBroadcast.builder().message("Привет").build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void runTransactionsInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(mock(TransactionStatus.class)));
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        broadcast.setId(10L);
        when(broadcastRepository.save(any())).thenReturn(broadcast);
        when(broadcastRepository.findById(10L)).thenReturn(Optional.of(broadcast));
    }

    @Test
    void sendsToActiveUsersAndCountsResults() throws InterruptedException {
        when(identityMsService.findBroadcastTargets(List.of())).thenReturn(List.of(
            new BroadcastTarget(1L, "tg-1", BotStatus.ACTIVE),
            new BroadcastTarget(2L, null, BotStatus.ACTIVE),
            new BroadcastTarget(3L, "tg-3", BotStatus.BLOCKED),
            new BroadcastTarget(4L, "tg-4", BotStatus.ACTIVE)));
        when(cryptoService.decrypt("tg-1")).thenReturn("111");
        when(cryptoService.decrypt("tg-4")).thenReturn("444");
        when(notificationService.sendNotification(any())).thenAnswer(invocation -> {
            NotificationDto dto = invocation.getArgument(0);
            NotificationDispatchStatus status = "111".equals(dto.getReceiver()) ? NotificationDispatchStatus.SENT : NotificationDispatchStatus.FAILED;
            return List.of(new NotificationDispatchResult(NotificationEngine.TELEGRAM_BOT, status, status == NotificationDispatchStatus.FAILED ? "403" : null));
        });

        BroadcastResponse response = service.broadcast("Привет", null, true, 99L);
        service.shutdown();

        assertThat(response.getBroadcastId()).isEqualTo(10L);
        assertThat(response.getTotalRecipients()).isEqualTo(4);

        ArgumentCaptor<NotificationDto> sent = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService, times(2)).sendNotification(sent.capture());
        assertThat(sent.getAllValues()).allSatisfy(dto -> {
            assertThat(dto.getType()).isEqualTo(NotificationType.ADMIN_BROADCAST);
            assertThat(dto.isPin()).isTrue();
            assertThat(dto.getDeduplicationKey()).startsWith("broadcast:10:chat:");
        });

        assertThat(broadcast.getSentCount()).isEqualTo(1);
        assertThat(broadcast.getFailedCount()).isEqualTo(1);
        assertThat(broadcast.getSkippedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationBroadcastRecipient>> recipients = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(recipients.capture());
        assertThat(recipients.getValue()).extracting(NotificationBroadcastRecipient::getUserId, NotificationBroadcastRecipient::getStatus)
            .containsExactly(
                tuple(1L, BroadcastRecipientStatus.SENT),
                tuple(2L, BroadcastRecipientStatus.SKIPPED),
                tuple(3L, BroadcastRecipientStatus.SKIPPED),
                tuple(4L, BroadcastRecipientStatus.FAILED));
    }

    @Test
    void broadcastWithoutRecipientsIsRejected() {
        when(identityMsService.findBroadcastTargets(List.of(5L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.broadcast("Привет", List.of(5L), false, 99L)).isInstanceOf(AppBadRequestException.class);
        verify(broadcastRepository, never()).save(any());
    }

    @Test
    void recipientsWithoutUserFilterAreNotNarrowed() {
        when(broadcastRepository.existsById(3L)).thenReturn(true);
        when(recipientRepository.findAllByBroadcastId(3L, PageRequest.of(0, 50))).thenReturn(Page.empty());

        service.findRecipients(3L, null, PageRequest.of(0, 50));

        verify(recipientRepository).findAllByBroadcastId(3L, PageRequest.of(0, 50));
        verify(recipientRepository, never()).findAllByBroadcastIdAndUserIdIn(any(), any(), any());
    }

    @Test
    void recipientsAreFilteredByUserIds() {
        when(broadcastRepository.existsById(3L)).thenReturn(true);
        when(recipientRepository.findAllByBroadcastIdAndUserIdIn(3L, List.of(7L, 8L), PageRequest.of(0, 50))).thenReturn(Page.empty());

        service.findRecipients(3L, List.of(7L, 8L), PageRequest.of(0, 50));

        verify(recipientRepository).findAllByBroadcastIdAndUserIdIn(3L, List.of(7L, 8L), PageRequest.of(0, 50));
        verify(recipientRepository, never()).findAllByBroadcastId(any(), any());
    }
}
