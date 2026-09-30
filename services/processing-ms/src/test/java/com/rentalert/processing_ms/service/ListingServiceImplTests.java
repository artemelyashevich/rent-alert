package com.rentalert.processing_ms.service;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.ParserResponseDto;
import com.rentalert.processing_ms.mapper.ListingEventMapper;
import com.rentalert.processing_ms.mapper.ParserResponseMapper;
import com.rentalert.processing_ms.messaging.ListingEventPublisher;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.service.impl.ListingServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingServiceImplTests {

    private final ListingRepository repository = mock(ListingRepository.class);
    private final ListingEventPublisher publisher = mock(ListingEventPublisher.class);
    private final ParserResponseMapper parserResponseMapper = mock(ParserResponseMapper.class);
    private final SchedulerSettingsService schedulerSettingsService = mock(SchedulerSettingsService.class);
    private final ListingService service = new ListingServiceImpl(repository,
        Mappers.getMapper(ListingEventMapper.class), publisher, parserResponseMapper, schedulerSettingsService);

    private final Listing listing = Listing.builder()
        .source(ScheduleProvider.KUFAR)
        .sourceId("100")
        .photos(new String[]{"a.jpg", "b.jpg"})
        .build();

    @BeforeEach
    void setUp() {
        Listing saved = Listing.builder()
            .source(ScheduleProvider.KUFAR)
            .sourceId("100")
            .sourceUrl("https://kufar.by/item/100")
            .photos(new String[]{"a.jpg", "b.jpg"})
            .build();
        saved.setId(7L);
        when(repository.saveAndFlush(listing)).thenReturn(saved);
    }

    @Test
    void publishesSavedListingWithItsId() {
        when(parserResponseMapper.toDto(any())).thenReturn(new ParserResponseDto());

        service.saveNew(listing);

        verify(publisher).publishListingCreated(argThat((ListingCreatedEvent event) ->
            event.listingId() == 7L
                && event.source() == ScheduleProvider.KUFAR
                && "100".equals(event.sourceId())
                && event.photos().equals(List.of("a.jpg", "b.jpg"))));
    }

    @Test
    void publishesConvertedPricesAndMessage() {
        ParserResponseDto dto = new ParserResponseDto();
        dto.setSource(ScheduleProvider.KUFAR);
        dto.setLink("https://kufar.by/item/100");
        dto.setPriceUsd(new BigDecimal("300"));
        dto.setPriceByn(new BigDecimal("990"));
        when(parserResponseMapper.toDto(any())).thenReturn(dto);

        service.saveNew(listing);

        verify(publisher).publishListingCreated(argThat((ListingCreatedEvent event) ->
            new BigDecimal("300").equals(event.priceUsd())
                && new BigDecimal("990").equals(event.priceByn())
                && event.message() != null
                && event.message().contains("300.00 $")
                && event.message().contains("https://kufar.by/item/100")));
    }

    @Test
    void publishesListingWithoutExtrasWhenTheyCannotBeBuilt() {
        when(parserResponseMapper.toDto(any())).thenThrow(new IllegalStateException("redis is down"));

        service.saveNew(listing);

        verify(publisher).publishListingCreated(argThat((ListingCreatedEvent event) ->
            event.listingId() == 7L
                && event.priceUsd() == null
                && event.priceByn() == null
                && event.message() == null));
    }

    @Test
    void alertsAdminWhenProviderNotificationsAreOn() {
        givenMessage();
        givenAdminNotifications(true);

        service.saveNew(listing);

        verify(publisher).publishNotificationRequest(argThat((NotificationRequest request) ->
            request.type() == NotificationType.NEW_LISTING_ADMIN_ALERT
                && request.listingId() == 7L
                && "listing:7:admin".equals(request.deduplicationKey())
                && request.templateParams().get("message").contains("https://kufar.by/item/100")));
    }

    @Test
    void noAdminAlertWhenProviderNotificationsAreOff() {
        givenMessage();
        givenAdminNotifications(false);

        service.saveNew(listing);

        verify(publisher, never()).publishNotificationRequest(any());
    }

    @Test
    void noAdminAlertWithoutMessage() {
        when(parserResponseMapper.toDto(any())).thenThrow(new IllegalStateException("redis is down"));
        givenAdminNotifications(true);

        service.saveNew(listing);

        verify(publisher, never()).publishNotificationRequest(any());
    }

    @Test
    void listingIsPublishedWhenAdminSettingsCannotBeRead() {
        givenMessage();
        when(schedulerSettingsService.findByProvider(ScheduleProvider.KUFAR)).thenThrow(new IllegalStateException("redis is down"));

        service.saveNew(listing);

        verify(publisher).publishListingCreated(any());
        verify(publisher, never()).publishNotificationRequest(any());
    }

    private void givenMessage() {
        ParserResponseDto dto = new ParserResponseDto();
        dto.setSource(ScheduleProvider.KUFAR);
        dto.setLink("https://kufar.by/item/100");
        when(parserResponseMapper.toDto(any())).thenReturn(dto);
    }

    private void givenAdminNotifications(boolean enabled) {
        SchedulerSettingsResponse settings = new SchedulerSettingsResponse();
        settings.setNotificationsEnabled(enabled);
        when(schedulerSettingsService.findByProvider(ScheduleProvider.KUFAR)).thenReturn(settings);
    }
}
