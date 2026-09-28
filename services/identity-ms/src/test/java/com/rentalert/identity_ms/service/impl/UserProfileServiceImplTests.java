package com.rentalert.identity_ms.service.impl;

import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
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
    private CacheService cacheService;

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

        verify(cacheService).evict(CacheType.USER_PROFILE, 5L);
    }

    @Test
    void evictsOnlyAfterCommitInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        userProfileService.evictCachedProfile(5L);
        verify(cacheService, never()).evict(CacheType.USER_PROFILE, 5L);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cacheService).evict(CacheType.USER_PROFILE, 5L);
    }
}
