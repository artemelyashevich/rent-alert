package com.rentalert.listing_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.NoActiveFiltersException;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.user.UserSession;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.mapper.ListingEventMapper;
import com.rentalert.listing_ms.mapper.ListingMapper;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.impl.ListingServiceImpl;
import com.rentalert.redis.service.UserSessionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ListingServiceImplTests {

    private final ListingRepository repository = mock(ListingRepository.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final ListingService service = new ListingServiceImpl(
        repository,
        Mappers.getMapper(ListingMapper.class),
        Mappers.getMapper(ListingEventMapper.class),
        userSessionService,
        mock(CurrencyRateService.class),
        identityMsService
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

    @Test
    void userWithoutActiveFilterGetsNoActiveFilters() {
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(42L).username("ann").build());
        when(identityMsService.getActiveFilter(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll(null, null, null, Pageable.unpaged()))
            .isInstanceOf(NoActiveFiltersException.class);
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
