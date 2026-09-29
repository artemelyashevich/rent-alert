package com.rentalert.core_ms.mapper;

import com.rentalert.core_ms.domain.ListingStatus;
import com.rentalert.core_ms.dto.ListingStatusResponseDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ListingStatusMapper {

    List<ListingStatusResponseDto> toDto(List<ListingStatus> statusList);

    ListingStatusResponseDto toDto(ListingStatus listingStatus);
}
