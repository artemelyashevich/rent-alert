package com.rentalert.listing_ms.mapper;

import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.dto.ListingStatusResponseDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ListingStatusMapper {

    List<ListingStatusResponseDto> toDto(List<ListingStatus> statusList);

    ListingStatusResponseDto toDto(ListingStatus listingStatus);
}
