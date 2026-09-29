package com.rentalert.processing_ms.service.impl;

import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.mapper.ListingEventMapper;
import com.rentalert.processing_ms.messaging.ListingEventPublisher;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ListingEventMapper listingEventMapper;
    private final ListingEventPublisher listingEventPublisher;

    @Override
    @Transactional
    public Listing saveNew(Listing listing) {
        Listing saved = listingRepository.saveAndFlush(listing);
        listingEventPublisher.publishListingCreated(listingEventMapper.toCreatedEvent(saved));
        return saved;
    }
}
