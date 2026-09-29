package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.listing_ms.domain.HiddenListing;
import com.rentalert.listing_ms.domain.HiddenListingEmbeddedId;
import com.rentalert.listing_ms.mapper.ListingMapper;
import com.rentalert.listing_ms.messaging.OpsEventPublisher;
import com.rentalert.listing_ms.repository.HiddenListingRepository;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.HiddenListingService;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.ListingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HiddenListingServiceImpl implements HiddenListingService {

    private final HiddenListingRepository hiddenListingRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final OpsEventPublisher opsEventPublisher;

    @Override
    @Transactional
    public void hideListing(Long userId, Long listingId) {
        log.debug("Hiding listing {} for user {}", listingId, userId);

        if (!listingRepository.existsById(listingId)) {
            throw new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        if (hiddenListingRepository.existsByIdUserIdAndIdListingId(userId, listingId)) {
            throw new AppException(InternalExceptionCode.EX400_HIDDEN_LISTING_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        HiddenListing hiddenListing = HiddenListing.builder()
                .id(HiddenListingEmbeddedId.builder().userId(userId).listingId(listingId).build())
                .listing(listingRepository.getReferenceById(listingId))
                .build();

        try {
            hiddenListingRepository.save(hiddenListing);
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition on hideListing detected: user={}, listing={}", userId, listingId);
            throw new AppException(InternalExceptionCode.EX400_HIDDEN_LISTING_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        log.info("Listing {} hidden for user {}", listingId, userId);
        opsEventPublisher.publishUserEvent(UserEvent.HIDDEN_LISTING, userId, listingId);
    }

    @Override
    public Page<ListingDto> getUserHiddenListings(Long userId, Pageable pageable) {
        Page<HiddenListing> hiddenListings = hiddenListingRepository.findAllByUserIdWithListing(userId, pageable);
        return hiddenListings.map(hiddenListing -> listingMapper.toDto(hiddenListing.getListing()));
    }

    @Override
    @Transactional
    public void removeFromHidden(Long userId, Long listingId) {
        log.debug("Removing listing {} from hidden for user {}", listingId, userId);

        HiddenListingEmbeddedId hiddenListingEmbeddedId = HiddenListingEmbeddedId.builder()
                .userId(userId)
                .listingId(listingId)
                .build();

        if (!hiddenListingRepository.existsById(hiddenListingEmbeddedId)) {
            throw new AppException(InternalExceptionCode.EX404_HIDDEN_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        hiddenListingRepository.deleteById(hiddenListingEmbeddedId);
        log.info("Listing {} removed from hidden for user {}", listingId, userId);
    }
}
