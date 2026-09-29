package com.rentalert.identity_ms.service.impl;

import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTests {

    @Mock
    private ProfileCacheEvictor profileCacheEvictor;

    @Mock
    private UserProfileRepository userProfileRepository;

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
}
