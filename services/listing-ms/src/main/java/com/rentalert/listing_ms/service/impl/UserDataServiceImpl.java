package com.rentalert.listing_ms.service.impl;

import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.HiddenListingRepository;
import com.rentalert.listing_ms.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataServiceImpl implements UserDataService {

    private final FavoriteRepository favoriteRepository;
    private final HiddenListingRepository hiddenListingRepository;

    @Override
    @Transactional
    public void deleteUserData(Long userId) {
        int favorites = favoriteRepository.deleteByUserId(userId);
        int hidden = hiddenListingRepository.deleteByUserId(userId);
        log.info("Deleted data of user {}: favorites={}, hidden={}", userId, favorites, hidden);
    }
}
