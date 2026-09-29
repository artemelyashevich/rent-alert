package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.mapper.DetailedUserProfileMapper;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.repository.DetailedUserProfileViewRepository;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserProfileCacheEvictionTests {

    private final CacheService cacheService = mock(CacheService.class);
    private final UserProfileServiceImpl service = new UserProfileServiceImpl(
        mock(UserProfileRepository.class),
        mock(DetailedUserProfileViewRepository.class),
        mock(DetailedUserProfileMapper.class),
        cacheService,
        mock(CryptoService.class),
        mock(UserProfileMapper.class),
        mock(HmacBlindIndexService.class),
        mock(UserSessionService.class),
        mock(TransactionTemplate.class)
    );

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsProfileAndInternalCachesOfOtherServices() {
        service.evictCachedProfile(42L);

        verify(cacheService).evict(CacheType.USER_PROFILE, 42L);
        verify(cacheService).evict(CacheType.USER_PROFILE_INTERNAL, 42L);
        verify(cacheService).evict(CacheType.USER_ACTIVE_FILTER, 42L);
    }

    @Test
    void insideTransactionEvictsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.evictCachedProfile(42L);
        verify(cacheService, never()).evict(any(), anyLong());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cacheService).evict(CacheType.USER_ACTIVE_FILTER, 42L);
    }

    @Test
    void saveEvictsCaches() {
        UserProfile profile = new UserProfile();
        profile.setId(42L);

        service.save(profile);

        verify(cacheService).evict(CacheType.USER_PROFILE_INTERNAL, 42L);
    }
}
