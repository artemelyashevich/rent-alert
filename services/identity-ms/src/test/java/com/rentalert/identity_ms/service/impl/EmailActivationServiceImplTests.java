package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.auth.EmailOtp;
import com.rentalert.dto.auth.OtpConfirmationRequest;
import com.rentalert.dto.auth.OtpPreparationResponse;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.messaging.NotificationRequestPublisher;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailActivationServiceImplTests {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final CryptoService cryptoService = mock(CryptoService.class);
    private final NotificationRequestPublisher notificationRequestPublisher = mock(NotificationRequestPublisher.class);
    private final EmailActivationServiceImpl service = new EmailActivationServiceImpl(repository, userProfileService, cacheService,
        cryptoService, notificationRequestPublisher, Duration.ofMinutes(10));

    private UserProfile profile;

    private static OtpConfirmationRequest confirm(String code) {
        return new OtpConfirmationRequest("req-1", code);
    }

    @BeforeEach
    void setUp() {
        profile = new UserProfile();
        profile.setId(5L);
        profile.setUsername("anna");
        profile.setSettings(UserSettings.builder().build());
        when(repository.findById(5L)).thenReturn(Optional.of(profile));
        when(cryptoService.encrypt(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));
    }

    @Test
    void preparationStoresCodeAndMailsItWithoutReturningIt() {
        OtpPreparationResponse response = service.prepare(5L, "anna@mail.by");

        assertThat(profile.getSettings().getEmail()).isEqualTo("anna@mail.by");
        assertThat(profile.getSettings().getEmailActive()).isFalse();

        ArgumentCaptor<EmailOtp> otp = ArgumentCaptor.forClass(EmailOtp.class);
        verify(cacheService).put(eq(CacheType.EMAIL_OTP), eq(response.requestId()), otp.capture(), eq(Duration.ofMinutes(10)));
        assertThat(otp.getValue().code()).matches("\\d{6}");
        assertThat(otp.getValue().userId()).isEqualTo(5L);

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationRequestPublisher).publishRequest(request.capture());
        NotificationRequest mail = request.getValue();
        assertThat(mail.type()).isEqualTo(NotificationType.EMAIL_ACTIVATION);
        assertThat(mail.templateParams()).containsEntry("otp", otp.getValue().code());
        assertThat(mail.receiver().emailEnc()).isEqualTo("enc:anna@mail.by");
        assertThat(mail.expiresAt()).isEqualTo(response.expiresAt());
    }

    @Test
    void activeEmailCannotBeReplaced() {
        profile.getSettings().setEmail("old@mail.by");
        profile.getSettings().setEmailActive(true);

        assertThatThrownBy(() -> service.prepare(5L, "anna@mail.by"))
            .isInstanceOf(AppBadRequestException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX400_EMAIL_ALREADY_ACTIVE);
        verify(notificationRequestPublisher, never()).publishRequest(any());
    }

    @Test
    void correctCodeActivatesEmail() {
        profile.getSettings().setEmail("anna@mail.by");
        when(cacheService.get(CacheType.EMAIL_OTP, "req-1"))
            .thenReturn(Optional.of(new EmailOtp(5L, "anna@mail.by", "123456", 0, Instant.now().plusSeconds(60))));

        service.activate(5L, confirm("123456"));

        assertThat(profile.getSettings().getEmailActive()).isTrue();
        verify(cacheService).evict(CacheType.EMAIL_OTP, "req-1");
        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationRequestPublisher).publishRequest(request.capture());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.EMAIL_ACTIVATION_SUCCESS);
        assertThat(request.getValue().templateParams()).containsEntry("login", "anna");
    }

    @Test
    void codeOfAnotherUserIsRejected() {
        when(cacheService.get(CacheType.EMAIL_OTP, "req-1"))
            .thenReturn(Optional.of(new EmailOtp(7L, "anna@mail.by", "123456", 0, Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.activate(5L, confirm("123456")))
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX400_OTP_INVALID);
        assertThat(profile.getSettings().getEmailActive()).isFalse();
    }

    @Test
    void wrongCodeCountsAttempt() {
        when(cacheService.get(CacheType.EMAIL_OTP, "req-1"))
            .thenReturn(Optional.of(new EmailOtp(5L, "anna@mail.by", "123456", 1, Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.activate(5L, confirm("000000")))
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX400_OTP_INVALID);

        ArgumentCaptor<EmailOtp> stored = ArgumentCaptor.forClass(EmailOtp.class);
        verify(cacheService).put(eq(CacheType.EMAIL_OTP), eq("req-1"), stored.capture(), any(Duration.class));
        assertThat(stored.getValue().failedAttempts()).isEqualTo(2);
    }

    @Test
    void fifthWrongCodeRevokesIt() {
        when(cacheService.get(CacheType.EMAIL_OTP, "req-1"))
            .thenReturn(Optional.of(new EmailOtp(5L, "anna@mail.by", "123456", 4, Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.activate(5L, confirm("000000"))).isInstanceOf(AppBadRequestException.class);

        verify(cacheService).evict(CacheType.EMAIL_OTP, "req-1");
        verify(cacheService, never()).put(any(), any(), any(), any(Duration.class));
    }

    @Test
    void codeForReplacedEmailIsRejected() {
        profile.getSettings().setEmail("new@mail.by");
        when(cacheService.get(CacheType.EMAIL_OTP, "req-1"))
            .thenReturn(Optional.of(new EmailOtp(5L, "anna@mail.by", "123456", 0, Instant.now().plusSeconds(60))));

        assertThatThrownBy(() -> service.activate(5L, confirm("123456"))).isInstanceOf(AppBadRequestException.class);
        assertThat(profile.getSettings().getEmailActive()).isFalse();
    }
}
