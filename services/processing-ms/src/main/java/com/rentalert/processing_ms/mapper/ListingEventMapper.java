package com.rentalert.processing_ms.mapper;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.processing_ms.domain.Listing;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ListingEventMapper {

    @Mapping(target = "listingId", source = "listing.id")
    ListingCreatedEvent toCreatedEvent(Listing listing, BigDecimal priceUsd, BigDecimal priceByn, String message);
}
