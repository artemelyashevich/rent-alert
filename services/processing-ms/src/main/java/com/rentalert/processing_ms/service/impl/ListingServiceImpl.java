package com.rentalert.processing_ms.service.impl;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.ParserResponseDto;
import com.rentalert.processing_ms.mapper.ListingEventMapper;
import com.rentalert.processing_ms.mapper.ParserResponseMapper;
import com.rentalert.processing_ms.messaging.ListingEventPublisher;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.service.ListingService;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import com.rentalert.processing_ms.util.ParserMessageUtil;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ListingEventMapper listingEventMapper;
    private final ListingEventPublisher listingEventPublisher;
    private final ParserResponseMapper parserResponseMapper;
    private final SchedulerSettingsService schedulerSettingsService;

    @Override
    @Transactional
    public Listing saveNew(Listing listing) {
        Listing saved = listingRepository.saveAndFlush(listing);
        ListingCreatedEvent event = toCreatedEvent(saved);
        listingEventPublisher.publishListingCreated(event);
        if (event.message() != null && isAdminAlertEnabled(saved)) {
            listingEventPublisher.publishNotificationRequest(NotificationRequest.builder()
                .producer("processing-ms")
                .type(NotificationType.NEW_LISTING_ADMIN_ALERT)
                .templateParams(Map.of("message", event.message()))
                .deduplicationKey("listing:%d:admin".formatted(saved.getId()))
                .listingId(saved.getId())
                .build());
        }
        return saved;
    }

    private boolean isAdminAlertEnabled(Listing listing) {
        try {
            return Boolean.TRUE.equals(
                schedulerSettingsService.findByProvider(listing.getSource()).getNotificationsEnabled());
        } catch (RuntimeException e) {
            log.warn("Failed to read admin alert settings for {}, listing {} is saved without the alert",
                listing.getSource(), listing.getId(), e);
            return false;
        }
    }

    // Converted prices and the notification text are extras: failing to build them mustn't lose the listing
    private ListingCreatedEvent toCreatedEvent(Listing listing) {
        try {
            ParserResponseDto dto = parserResponseMapper.toDto(listing);
            return listingEventMapper.toCreatedEvent(listing, dto.getPriceUsd(), dto.getPriceByn(),
                ParserMessageUtil.buildMessage(dto));
        } catch (RuntimeException e) {
            log.warn("Failed to build prices and message for listing {}, publishing it without them", listing.getId(), e);
            return listingEventMapper.toCreatedEvent(listing, null, null, null);
        }
    }
}
