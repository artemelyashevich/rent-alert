package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.auth.EmailOtp;
import com.rentalert.dto.auth.OtpConfirmationRequest;
import com.rentalert.dto.auth.OtpPreparationResponse;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.EmailActivationService;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailActivationServiceImpl implements EmailActivationService {

    private static final String PRODUCER = "identity-ms";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final CacheService cacheService;
    private final CryptoService cryptoService;
    private final OutboxPublisher outboxPublisher;
    private final Duration otpTtl;

    public EmailActivationServiceImpl(UserProfileRepository userProfileRepository, UserProfileService userProfileService,
        CacheService cacheService, CryptoService cryptoService, OutboxPublisher outboxPublisher,
        @Value("${app.email.otp-ttl:PT10M}") Duration otpTtl) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
        this.cacheService = cacheService;
        this.cryptoService = cryptoService;
        this.outboxPublisher = outboxPublisher;
        this.otpTtl = otpTtl;
    }

    private static AppBadRequestException invalidOtp() {
        return new AppBadRequestException(InternalExceptionCode.EX400_OTP_INVALID);
    }

    @Override
    @Transactional
    public OtpPreparationResponse prepare(Long userId, String email) {
        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(AppNotFoundException::new);
        UserSettings settings = profile.getSettings();
        if (Boolean.TRUE.equals(settings.getEmailActive())) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_EMAIL_ALREADY_ACTIVE);
        }
        settings.setEmail(email);
        settings.setEmailActive(false);
        userProfileRepository.save(profile);
        userProfileService.evictCachedProfile(userId);

        String requestId = UUID.randomUUID().toString();
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(otpTtl);
        cacheService.put(CacheType.EMAIL_OTP, requestId, new EmailOtp(userId, email, code, 0, expiresAt), otpTtl);

        publish(NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(userId)
            .type(NotificationType.EMAIL_ACTIVATION)
            .templateParams(Map.of("otp", code))
            .deduplicationKey("email-activation:" + requestId)
            .receiver(new NotificationRequest.Receiver(null, cryptoService.encrypt(email)))
            .expiresAt(expiresAt)
            .build());
        log.info("Email activation code sent to user {}", userId);
        return new OtpPreparationResponse(requestId, expiresAt);
    }

    @Override
    @Transactional
    public void activate(Long userId, OtpConfirmationRequest request) {
        EmailOtp otp = cacheService.<EmailOtp>get(CacheType.EMAIL_OTP, request.requestId())
            .filter(stored -> stored.userId().equals(userId))
            .orElseThrow(EmailActivationServiceImpl::invalidOtp);

        if (!MessageDigest.isEqual(otp.code().getBytes(StandardCharsets.UTF_8), request.code().getBytes(StandardCharsets.UTF_8))) {
            registerFailedAttempt(request.requestId(), otp);
            throw invalidOtp();
        }
        cacheService.evict(CacheType.EMAIL_OTP, request.requestId());

        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(AppNotFoundException::new);
        UserSettings settings = profile.getSettings();
        if (!otp.email().equals(settings.getEmail())) {
            throw invalidOtp();
        }
        settings.setEmailActive(true);
        userProfileRepository.save(profile);
        userProfileService.evictCachedProfile(userId);

        publish(NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(userId)
            .type(NotificationType.EMAIL_ACTIVATION_SUCCESS)
            .templateParams(Map.of("login", Optional.ofNullable(profile.getUsername()).orElse(String.valueOf(userId))))
            .deduplicationKey("email-activation-success:" + request.requestId())
            .receiver(new NotificationRequest.Receiver(null, cryptoService.encrypt(settings.getEmail())))
            .build());
        log.info("Email of user {} confirmed", userId);
    }

    private void registerFailedAttempt(String requestId, EmailOtp otp) {
        EmailOtp failed = otp.withFailedAttempt();
        Duration remaining = Duration.between(Instant.now(), otp.expiresAt());
        if (failed.failedAttempts() >= MAX_FAILED_ATTEMPTS || remaining.isNegative() || remaining.isZero()) {
            cacheService.evict(CacheType.EMAIL_OTP, requestId);
            log.warn("Email activation code {} of user {} revoked after {} failed attempts", requestId, otp.userId(), failed.failedAttempts());
            return;
        }
        cacheService.put(CacheType.EMAIL_OTP, requestId, failed, remaining);
    }

    private void publish(NotificationRequest request) {
        outboxPublisher.publish(KafkaTopics.notificationRequests(request.type()), String.valueOf(request.userId()), request);
    }
}
