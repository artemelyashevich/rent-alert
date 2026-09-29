package com.rentalert.listing_ms.mapper;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.listing_ms.domain.Listing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ListingEventMapper {

    @Mapping(target = "id", source = "listingId")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Listing toEntity(ListingCreatedEvent event);
}
