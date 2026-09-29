package com.rentalert.listing_ms.mapper;

import com.rentalert.listing_ms.domain.CalendarEventView;
import com.rentalert.listing_ms.dto.CalendarEventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    @Mapping(target = "listingId", source = "id.listingId")
    @Mapping(target = "address", expression = "java(buildAddress(view.getStreet(), view.getMetroStation()))")
    CalendarEventDto toDto(CalendarEventView view);

    default String buildAddress(String street, String metroStation) {
        if (street == null || street.isBlank()) {
            return metroStation;
        }
        if (metroStation == null || metroStation.isBlank()) {
            return street;
        }
        return street + " · м. " + metroStation;
    }
}
