package com.rentalert.listing_ms.service;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.mapper.ListingEventMapper;
import com.rentalert.listing_ms.mapper.ListingMapper;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.impl.ListingServiceImpl;
import com.rentalert.redis.service.UserSessionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingServiceImplTests {

    private final ListingRepository repository = mock(ListingRepository.class);
    private final ListingService service = new ListingServiceImpl(
        repository,
        Mappers.getMapper(ListingMapper.class),
        Mappers.getMapper(ListingEventMapper.class),
        mock(UserSessionService.class),
        mock(CurrencyRateService.class)
    );

    private final ListingCreatedEvent event = ListingCreatedEvent.builder()
        .listingId(7L)
        .source(ScheduleProvider.ONLINER)
        .sourceId("42")
        .photos(List.of("a.jpg"))
        .build();

    @Test
    void storesListingWithIdFromProcessing() {
        service.saveCreated(event);

        verify(repository).save(argThat((Listing listing) -> listing.getId() == 7L
            && listing.isNew()
            && listing.isActive()
            && listing.getPhotos().length == 1));
    }

    @Test
    void ignoresRedeliveredListing() {
        when(repository.existsById(7L)).thenReturn(true);

        service.saveCreated(event);

        verify(repository, never()).save(any());
    }

    @Test
    void mappedListingKeepsSource() {
        Listing listing = Mappers.getMapper(ListingEventMapper.class).toEntity(event);

        assertThat(listing.getSource()).isEqualTo(ScheduleProvider.ONLINER);
        assertThat(listing.getSourceId()).isEqualTo("42");
    }
}
