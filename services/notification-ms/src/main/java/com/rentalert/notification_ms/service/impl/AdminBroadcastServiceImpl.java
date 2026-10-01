package com.rentalert.notification_ms.service.impl;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRecipientStatus;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationDispatchResult;
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
import com.rentalert.notification_ms.service.AdminBroadcastService;
import com.rentalert.notification_ms.service.NotificationService;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminBroadcastServiceImpl implements AdminBroadcastService {

    private static final int CHUNK_SIZE = 75;

    private final IdentityMsService identityMsService;
    private final NotificationBroadcastRepository broadcastRepository;
    private final NotificationBroadcastRecipientRepository recipientRepository;
    private final NotificationBroadcastMapper broadcastMapper;
    private final NotificationBroadcastRecipientMapper recipientMapper;
    private final NotificationService notificationService;
    private final CryptoService cryptoService;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService broadcastExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("broadcast-", 0).factory());

    public AdminBroadcastServiceImpl(IdentityMsService identityMsService, NotificationBroadcastRepository broadcastRepository,
        NotificationBroadcastRecipientRepository recipientRepository, NotificationBroadcastMapper broadcastMapper,
        NotificationBroadcastRecipientMapper recipientMapper, NotificationService notificationService, CryptoService cryptoService,
        TransactionTemplate transactionTemplate) {
        this.identityMsService = identityMsService;
        this.broadcastRepository = broadcastRepository;
        this.recipientRepository = recipientRepository;
        this.broadcastMapper = broadcastMapper;
        this.recipientMapper = recipientMapper;
        this.notificationService = notificationService;
        this.cryptoService = cryptoService;
        this.transactionTemplate = transactionTemplate;
    }

    private static <T> List<List<T>> partition(List<T> source, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            partitions.add(source.subList(i, Math.min(i + size, source.size())));
        }
        return partitions;
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        broadcastExecutor.shutdown();
        if (!broadcastExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Admin broadcast is still running on shutdown, remaining recipients are not sent");
            broadcastExecutor.shutdownNow();
        }
    }

    @Override
    public BroadcastResponse broadcast(String message, List<Long> userIds, boolean pin, Long adminUserId) {
        List<BroadcastTarget> targets = identityMsService.findBroadcastTargets(userIds == null ? List.of() : userIds);
        if (targets.isEmpty()) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_BROADCAST_NO_RECIPIENTS);
        }

        Long broadcastId = transactionTemplate.execute(status -> broadcastRepository.save(NotificationBroadcast.builder()
            .userId(adminUserId)
            .message(message)
            .totalRecipients(targets.size())
            .pinned(pin)
            .build()).getId());
        log.info("Admin broadcast {} accepted: recipients={}, pin={}", broadcastId, targets.size(), pin);

        broadcastExecutor.execute(() -> send(broadcastId, targets, message, pin));

        return BroadcastResponse.builder()
            .broadcastId(broadcastId)
            .totalRecipients(targets.size())
            .build();
    }

    private void send(Long broadcastId, List<BroadcastTarget> targets, String message, boolean pin) {
        try {
            for (List<BroadcastTarget> chunk : partition(targets, CHUNK_SIZE)) {
                List<RecipientResult> results = chunk.stream()
                    .map(target -> sendSafely(target, message, broadcastId, pin))
                    .toList();
                saveResults(broadcastId, results);
            }
            log.info("Admin broadcast {} finished", broadcastId);
        } catch (Exception e) {
            log.error("Admin broadcast {} stopped: {}", broadcastId, e.getMessage(), e);
        }
    }

    private void saveResults(Long broadcastId, List<RecipientResult> results) {
        transactionTemplate.executeWithoutResult(status -> {
            NotificationBroadcast broadcast = broadcastRepository.findById(broadcastId).orElse(null);
            if (broadcast == null) {
                return;
            }
            recipientRepository.saveAll(results.stream()
                .map(result -> NotificationBroadcastRecipient.builder()
                    .broadcast(broadcast)
                    .userId(result.userId())
                    .status(result.status())
                    .errorMessage(result.reason())
                    .build())
                .toList());
            for (RecipientResult result : results) {
                switch (result.status()) {
                    case SENT -> broadcast.setSentCount(broadcast.getSentCount() + 1);
                    case FAILED -> broadcast.setFailedCount(broadcast.getFailedCount() + 1);
                    case SKIPPED -> broadcast.setSkippedCount(broadcast.getSkippedCount() + 1);
                }
            }
            broadcastRepository.save(broadcast);
        });
    }

    private RecipientResult sendSafely(BroadcastTarget target, String message, Long broadcastId, boolean pin) {
        try {
            return sendToRecipient(target, message, broadcastId, pin);
        } catch (Exception e) {
            log.error("Unexpected error while broadcasting to userId={}", target.userId(), e);
            return new RecipientResult(target.userId(), BroadcastRecipientStatus.FAILED, e.getMessage());
        }
    }

    private RecipientResult sendToRecipient(BroadcastTarget target, String message, Long broadcastId, boolean pin) {
        if (!StringUtils.hasText(target.telegramIdEnc())) {
            return new RecipientResult(target.userId(), BroadcastRecipientStatus.SKIPPED, "Telegram не привязан");
        }
        if (target.botStatus() == BotStatus.BLOCKED) {
            return new RecipientResult(target.userId(), BroadcastRecipientStatus.SKIPPED, "Бот не активен: " + target.botStatus());
        }
        String chatId = cryptoService.decrypt(target.telegramIdEnc());

        List<NotificationDispatchResult> dispatched = notificationService.sendNotification(NotificationDto.builder()
            .type(NotificationType.ADMIN_BROADCAST)
            .engine(NotificationEngine.TELEGRAM_BOT)
            .userId(target.userId())
            .receiver(chatId)
            .templateParams(Map.of("message", message))
            .deduplicationKey("broadcast:" + broadcastId + ":chat:" + chatId)
            .pin(pin)
            .build());
        if (dispatched.isEmpty()) {
            return new RecipientResult(target.userId(), BroadcastRecipientStatus.FAILED, "Нет доступных каналов доставки");
        }
        NotificationDispatchResult result = dispatched.getFirst();
        return switch (result.status()) {
            case SENT -> new RecipientResult(target.userId(), BroadcastRecipientStatus.SENT, null);
            case SKIPPED -> new RecipientResult(target.userId(), BroadcastRecipientStatus.SKIPPED, result.reason());
            case FAILED -> new RecipientResult(target.userId(), BroadcastRecipientStatus.FAILED, result.reason());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BroadcastSummaryResponse> findAll(Pageable pageable) {
        return broadcastRepository.findAllWithCreatedBy(pageable).map(broadcastMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public BroadcastSummaryResponse findById(Long broadcastId) {
        return broadcastRepository.findByIdWithCreatedBy(broadcastId)
            .map(broadcastMapper::toSummary)
            .orElseThrow(() -> new AppNotFoundException(InternalExceptionCode.EX404_BROADCAST_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BroadcastRecipientResponse> findRecipients(Long broadcastId, Long userId, Pageable pageable) {
        if (!broadcastRepository.existsById(broadcastId)) {
            throw new AppNotFoundException(InternalExceptionCode.EX404_BROADCAST_NOT_FOUND);
        }
        return recipientRepository.findAllByBroadcastId(broadcastId, userId, pageable).map(recipientMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long broadcastId) {
        if (!broadcastRepository.existsById(broadcastId)) {
            throw new AppNotFoundException(InternalExceptionCode.EX404_BROADCAST_NOT_FOUND);
        }
        broadcastRepository.deleteById(broadcastId);
        log.info("Admin broadcast {} deleted", broadcastId);
    }

    private record RecipientResult(Long userId, BroadcastRecipientStatus status, String reason) {

    }
}
