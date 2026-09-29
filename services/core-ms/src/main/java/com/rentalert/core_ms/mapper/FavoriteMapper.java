package com.rentalert.core_ms.mapper;

import com.rentalert.core_ms.domain.Favorite;
import com.rentalert.core_ms.domain.ListingStatus;
import com.rentalert.core_ms.dto.FavoriteResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ListingMapper.class})
public interface FavoriteMapper {

    @Mapping(source = "listing", target = "listing")
    @Mapping(source = "listingStatus", target = "status")
    FavoriteResponse toResponse(Favorite favorite);

    List<FavoriteResponse> toResponseList(List<Favorite> favorites);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "color", target = "color")
    @Mapping(source = "description", target = "description")
    FavoriteResponse.ListingStatusInfo toListingStatusInfo(ListingStatus status);
}
