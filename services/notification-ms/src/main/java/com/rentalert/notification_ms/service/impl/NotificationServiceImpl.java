package com.rentalert.notification_ms.service.impl;

import com.rentalert.common.exception.AppRateLimitException;
import com.rentalert.common.util.JsonHelper;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import com.rentalert.dto.notification.NotificationCategory;
import com.rentalert.dto.notification.NotificationDispatchResult;
import com.rentalert.dto.notification.NotificationDispatchStatus;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.domain.NotificationStatus;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import com.rentalert.notification_ms.mapper.NotificationMapper;
import com.rentalert.notification_ms.repository.NotificationRepository;
import com.rentalert.notification_ms.repository.NotificationTemplateRepository;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import com.rentalert.notification_ms.service.NotificationService;
import com.rentalert.notification_ms.service.NotificationStrategy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_ATTEMPT = 3;
    private static final long BASE_DELAY_SECONDS = 60;
    private static final long MAX_JITTER_SECONDS = 10;

    private final Map<NotificationEngine, NotificationStrategy> strategyMap;
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final NotificationMapper notificationMapper;
    private final CryptoService cryptoService;
    private final HmacBlindIndexService blindIndexService;

    public NotificationServiceImpl(
        List<NotificationStrategy> strategies,
        NotificationRepository notificationRepository,
        NotificationTemplateRepository notificationTemplateRepository,
        UserNotificationSettingRepository userNotificationSettingRepository,
        NotificationMapper notificationMapper, CryptoService cryptoService,
        HmacBlindIndexService blindIndexService
    ) {
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(NotificationStrategy::getSupportedEngine, s -> s));
        this.notificationRepository = notificationRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.userNotificationSettingRepository = userNotificationSettingRepository;
        this.notificationMapper = notificationMapper;
        this.cryptoService = cryptoService;
        this.blindIndexService = blindIndexService;
    }

    @Override
    public List<NotificationDispatchResult> sendNotification(NotificationDto dto) {
        if (dto == null || dto.getType() == null) {
            log.error("Notification attempt with null DTO or Type");
            return List.of();
        }
        if (dto.getReceiver() == null) {
            dto.setReceiver("[internal]");
        }

        dto.setDeduplicationKey(blindIndexService.hash(dto.getDeduplicationKey()));

        NotificationType type = dto.getType();
        List<NotificationEngine> engines = dto.getEngine() != null
            ? List.of(dto.getEngine())
            : type.getDefaultEngines();

        if (engines.isEmpty()) {
            log.warn("No engines resolved for type={}", type);
            return List.of();
        }

        List<NotificationDispatchResult> results = new ArrayList<>();
        for (NotificationEngine engine : engines) {
            if (!isEnabledForUser(dto.getUserId(), type, engine)) {
                log.debug("Engine {} disabled by user settings. userId={}, type={}", engine, dto.getUserId(), type);
                results.add(new NotificationDispatchResult(engine, NotificationDispatchStatus.SKIPPED, "disabled by user notification settings"));
                continue;
            }
            results.add(dispatchToEngine(dto, engine));
        }
        return results;
    }

    private boolean isEnabledForUser(Long userId, NotificationType type, NotificationEngine engine) {
        if (engine == NotificationEngine.WEBSOCKET) {
            return true;
        }
        NotificationCategory category = type.getCategory();
        if (category.isMandatory() && category.getMandatoryEngines().contains(engine)) {
            return true;
        }
        if (userId == null) {
            return true;
        }
        return userNotificationSettingRepository
            .findByUserIdAndEngine(userId, engine)
            .map(UserNotificationSetting::isEnabled)
            .orElse(true);
    }

    private NotificationDispatchResult dispatchToEngine(NotificationDto dto, NotificationEngine engine) {
        dto.setEngine(engine);

        if (isDuplicate(dto, engine)) {
            log.debug("Already sent, skipping. key={}, engine={}", dto.getDeduplicationKey(), engine);
            return new NotificationDispatchResult(engine, NotificationDispatchStatus.SKIPPED, "duplicate notification already sent");
        }

        NotificationStrategy strategy = strategyMap.get(engine);
        if (strategy == null) {
            log.error("No notification strategy found for engine: {}", engine);
            return new NotificationDispatchResult(engine, NotificationDispatchStatus.FAILED, "no strategy configured for engine");
        }

        String text;
        String templateName;
        try {
            TemplateResponse templateResponse = renderTemplate(dto.getType(), engine, dto.getTemplateParams());
            text = templateResponse.content();
            templateName = templateResponse.name();
        } catch (Exception e) {
            log.error("Template rendering failed for type={}, engine={}: {}", dto.getType(), engine, e.getMessage());
            persistNonRetryableFailure(dto, e.getMessage());
            return new NotificationDispatchResult(engine, NotificationDispatchStatus.FAILED, e.getMessage());
        }
        dto.setContent(text);
        dto.setTemplateName(templateName);
        try {
            strategy.sendNotification(dto);
            log.info("Successfully sent notification to {} via {}", dto.getUserId(), engine);
            saveSuccessSafely(dto);
            return new NotificationDispatchResult(engine, NotificationDispatchStatus.SENT, null);
        } catch (Exception e) {
            log.error("Failed to send notification to {} via {}. Error: {}",
                dto.getReceiver(), engine, e.getMessage());
            persistRetryableFailure(dto, e);
            return new NotificationDispatchResult(engine, NotificationDispatchStatus.FAILED, e.getMessage());
        }
    }

    @Override
    public void scheduleNotification(NotificationDto dto, Instant sendAt) {
        if (dto == null || dto.getType() == null) {
            log.error("Notification attempt with null DTO or Type");
            return;
        }
        if (dto.getReceiver() == null) {
            dto.setReceiver("[internal]");
        }

        dto.setDeduplicationKey(blindIndexService.hash(dto.getDeduplicationKey()));

        NotificationType type = dto.getType();
        List<NotificationEngine> engines = dto.getEngine() != null
            ? List.of(dto.getEngine())
            : type.getDefaultEngines();

        if (engines.isEmpty()) {
            log.warn("No engines resolved for type={}", type);
            return;
        }

        for (NotificationEngine engine : engines) {
            if (!isEnabledForUser(dto.getUserId(), type, engine)) {
                log.debug("Engine {} disabled by user settings. userId={}, type={}", engine, dto.getUserId(), type);
                continue;
            }
            dto.setEngine(engine);
            if (isDuplicate(dto, engine)) {
                log.debug("Already sent, skipping queue. key={}, engine={}", dto.getDeduplicationKey(), engine);
                continue;
            }
            persistQueued(dto, engine, sendAt);
        }
    }

    private void persistQueued(NotificationDto dto, NotificationEngine engine, Instant sendAt) {
        try {
            Notification notification = Notification.builder()
                .engine(engine)
                .receiver(dto.getReceiver())
                .status(NotificationStatus.QUEUED)
                .attempt(0)
                .nextRetryAt(sendAt)
                .retryType(dto.getType())
                .deduplicationKey(dto.getDeduplicationKey())
                .templateParamsJson(serializeParams(dto.getTemplateParams()))
                .listingId(dto.getListingId())
                .build();
            notificationRepository.save(notification);
            log.info("Queued notification for later delivery. receiver={}, type={}, sendAt={}", dto.getReceiver(), dto.getType(), sendAt);
        } catch (Exception e) {
            log.error("Failed to persist QUEUED notification for receiver={}", dto.getReceiver(), e);
        }
    }

    private boolean isDuplicate(NotificationDto dto, NotificationEngine engine) {
        return dto.getDeduplicationKey() != null
            && notificationRepository.existsByDeduplicationKeyAndEngineAndStatusIn(
            dto.getDeduplicationKey(), engine, List.of(NotificationStatus.SUCCESS));
    }

    private void saveSuccessSafely(NotificationDto dto) {
        try {
            Notification successEntity = notificationMapper.toSuccessEntity(dto);
            if (dto.getEngine().equals(NotificationEngine.TELEGRAM_BOT)) {
                successEntity.setReceiver(cryptoService.encrypt(dto.getReceiver()));
            }
            notificationRepository.save(successEntity);
        } catch (DataIntegrityViolationException race) {
            log.warn("Duplicate SUCCESS write suppressed for key={}, engine={}",
                dto.getDeduplicationKey(), dto.getEngine(), race);
        } catch (Exception dbEx) {
            log.error("Failed to persist SUCCESS notification for receiver={}", dto.getReceiver(), dbEx);
        }
    }

    private void persistNonRetryableFailure(NotificationDto dto, String errorMessage) {
        try {
            Notification notification = notificationMapper.toFailureEntity(dto, errorMessage);
            notification.setAttempt(MAX_ATTEMPT);
            notification.setNextRetryAt(null);
            notification.setStatus(NotificationStatus.EXHAUSTED);
            notification.setRetryType(dto.getType());
            notificationRepository.save(notification);
        } catch (Exception dbEx) {
            log.error("Failed to persist EXHAUSTED notification for receiver={}", dto.getReceiver(), dbEx);
        }
    }

    private void persistRetryableFailure(NotificationDto dto, Exception cause) {
        try {
            Notification notification = notificationMapper.toFailureEntity(dto, cause.getMessage());
            notification.setAttempt(1);
            notification.setNextRetryAt(calculateNextRetry(1, retryAfterSecondsOf(cause)));
            notification.setRetryType(dto.getType());
            notification.setTemplateParamsJson(serializeParams(dto.getTemplateParams()));
            notificationRepository.save(notification);
        } catch (Exception dbEx) {
            log.error("Failed to persist FAILED notification for receiver={}", dto.getReceiver(), dbEx);
        }
    }

    @Override
    public void retryFailed() {
        List<Notification> candidates = notificationRepository.findRetryable(
            List.of(NotificationStatus.FAILURE, NotificationStatus.QUEUED), MAX_ATTEMPT, Instant.now()
        );

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Retrying {} FAILED/QUEUED notifications", candidates.size());
        candidates.forEach(this::retryOne);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int deleteObsoleteNotifications(Instant cutoff) {
        int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} notification entries older than {}", deleted, cutoff);
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByListingIds(List<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return 0;
        }
        int deleted = notificationRepository.deleteAllByListingIdIn(listingIds);
        log.info("Deleted {} notification entries for {} listings", deleted, listingIds.size());
        return deleted;
    }

    private void retryOne(Notification notification) {
        NotificationStrategy strategy = strategyMap.get(notification.getEngine());
        if (strategy == null) {
            log.error("No strategy for engine={}. Exhausting. id={}", notification.getEngine(), notification.getId());
            notification.setStatus(NotificationStatus.EXHAUSTED);
            notificationRepository.save(notification);
            return;
        }

        if (notification.getDeduplicationKey() != null
            && notificationRepository.existsByDeduplicationKeyAndEngineAndStatusIn(
            notification.getDeduplicationKey(), notification.getEngine(), List.of(NotificationStatus.SUCCESS))) {
            log.info("Already delivered by another dispatch, exhausting. id={}", notification.getId());
            notification.setStatus(NotificationStatus.EXHAUSTED);
            notification.setErrorMessage("already delivered");
            notification.setNextRetryAt(null);
            notificationRepository.save(notification);
            return;
        }

        notification.setAttempt(notification.getAttempt() + 1);
        try {
            NotificationDto dto = notificationMapper.toDto(notification);
            Map<String, String> params = deserializeParams(notification.getTemplateParamsJson());
            TemplateResponse templateResponse = renderTemplate(notification.getRetryType(), notification.getEngine(), params);
            dto.setContent(templateResponse.content());
            dto.setTemplateName(templateResponse.name());
            strategy.sendNotification(dto);

            notification.setStatus(NotificationStatus.SUCCESS);
            notification.setErrorMessage(null);
            notification.setNextRetryAt(null);
            log.info("Retry succeeded. id={}, attempt={}", notification.getId(), notification.getAttempt());
        } catch (Exception e) {
            notification.setErrorMessage(e.getMessage());
            if (notification.getAttempt() >= MAX_ATTEMPT) {
                notification.setStatus(NotificationStatus.EXHAUSTED);
                log.warn("Retry exhausted after {} attempts. id={}", notification.getAttempt(), notification.getId());
            } else {
                notification.setStatus(NotificationStatus.FAILURE);
                notification.setNextRetryAt(calculateNextRetry(notification.getAttempt(), retryAfterSecondsOf(e)));
                log.warn("Retry failed (attempt={}/{}). id={}, error={}",
                    notification.getAttempt(), MAX_ATTEMPT, notification.getId(), e.getMessage());
            }
        } finally {
            notificationRepository.save(notification);
        }
    }

    private TemplateResponse renderTemplate(NotificationType type, NotificationEngine engine, Map<String, String> params) {
        NotificationTemplate template = notificationTemplateRepository
            .findByNotificationTypeAndEngine(type, engine)
            .orElseThrow(() -> new IllegalStateException(
                "No template configured for type=" + type + ", engine=" + engine));
        String text = template.getTemplateText();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return new TemplateResponse(text, template.getTemplateName());
    }

    private Instant calculateNextRetry(int attempt, Long overrideDelaySeconds) {
        long delaySeconds;
        if (overrideDelaySeconds != null && overrideDelaySeconds > 0) {
            delaySeconds = overrideDelaySeconds;
        } else {
            long exponentialDelay = BASE_DELAY_SECONDS * (1L << (attempt - 1));
            long jitter = ThreadLocalRandom.current().nextLong(MAX_JITTER_SECONDS + 1);
            delaySeconds = exponentialDelay + jitter;
        }
        return Instant.now().plusSeconds(delaySeconds);
    }

    private Long retryAfterSecondsOf(Throwable e) {
        return (e instanceof AppRateLimitException rateLimit) ? rateLimit.getRetryAfterSeconds() : null;
    }

    private String serializeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        return JsonHelper.toJson(params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> deserializeParams(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return (Map<String, String>) JsonHelper.fromJson(json, Map.class);
    }

    record TemplateResponse(String content, String name) {}
}