package com.rentalert.processing_ms.service;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.mapper.ListingEventMapper;
import com.rentalert.processing_ms.messaging.ListingEventPublisher;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.service.impl.ListingServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingServiceImplTests {

    private final ListingRepository repository = mock(ListingRepository.class);
    private final ListingEventPublisher publisher = mock(ListingEventPublisher.class);
    private final ListingService service =
        new ListingServiceImpl(repository, Mappers.getMapper(ListingEventMapper.class), publisher);

    @Test
    void publishesSavedListingWithItsId() {
        Listing listing = Listing.builder()
            .source(ScheduleProvider.KUFAR)
            .sourceId("100")
            .photos(new String[]{"a.jpg", "b.jpg"})
            .build();
        Listing saved = Listing.builder()
            .source(ScheduleProvider.KUFAR)
            .sourceId("100")
            .photos(new String[]{"a.jpg", "b.jpg"})
            .build();
        saved.setId(7L);
        when(repository.saveAndFlush(listing)).thenReturn(saved);

        service.saveNew(listing);

        verify(publisher).publishListingCreated(argThat((ListingCreatedEvent event) ->
            event.listingId() == 7L
                && event.source() == ScheduleProvider.KUFAR
                && "100".equals(event.sourceId())
                && event.photos().equals(List.of("a.jpg", "b.jpg"))));
    }
}
