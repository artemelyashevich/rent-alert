package com.rentalert.roommate_ms.mapper;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.roommate_ms.domain.RoommateSharedFavorite;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoommateSharedFavoriteMapper {

    @Mapping(target = "listing", source = "listing")
    @Mapping(target = "status", source = "favorite.listingStatus")
    RoommateSharedFavoriteResponse toResponse(RoommateSharedFavorite favorite, ListingDto listing);
}
