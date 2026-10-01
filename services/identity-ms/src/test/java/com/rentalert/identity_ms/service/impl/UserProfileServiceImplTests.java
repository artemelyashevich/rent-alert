package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTests {

    @Mock
    private ProfileCacheEvictor profileCacheEvictor;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsImmediatelyOutsideTransaction() {
        userProfileService.evictCachedProfile(5L);

        verify(profileCacheEvictor).evict(5L);
    }

    @Test
    void evictsOnlyAfterCommitInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        userProfileService.evictCachedProfile(5L);
        verify(profileCacheEvictor, never()).evict(5L);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(profileCacheEvictor).evict(5L);
    }

    @Test
    void saveEvictsCachedProfile() {
        UserProfile profile = new UserProfile();
        profile.setId(5L);

        userProfileService.save(profile);

        verify(profileCacheEvictor).evict(5L);
    }

    @Test
    void receiverHasEncryptedActiveEmail() {
        UserProfile profile = profileWithSettings(UserSettings.builder().email("user@mail.by").emailActive(true).build());
        when(userProfileRepository.findById(5L)).thenReturn(Optional.of(profile));
        when(cryptoService.encrypt("user@mail.by")).thenReturn("email-enc");

        assertThat(userProfileService.getReceiver(5L)).isEqualTo(new NotificationRequest.Receiver("tg-enc", "email-enc"));
    }

    @Test
    void receiverOmitsUnconfirmedEmail() {
        UserProfile profile = profileWithSettings(UserSettings.builder().email("user@mail.by").emailActive(false).build());
        when(userProfileRepository.findById(5L)).thenReturn(Optional.of(profile));

        assertThat(userProfileService.getReceiver(5L)).isEqualTo(new NotificationRequest.Receiver("tg-enc", null));
        verify(cryptoService, never()).encrypt("user@mail.by");
    }

    private UserProfile profileWithSettings(UserSettings settings) {
        UserProfile profile = new UserProfile();
        profile.setId(5L);
        profile.setTelegramId("tg-enc");
        profile.setSettings(settings);
        return profile;
    }
}
