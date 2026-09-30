package com.rentalert.listing_ms.mapper;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.listing_ms.domain.Listing;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    imports = {com.rentalert.dto.listing.ScheduleProvider.class}
)
public interface ListingMapper {

    @Mapping(target = "photos", expression = "java(toList(entity.getPhotos()))")
    @Mapping(target = "previewPhoto", expression = "java(firstOrNull(entity.getPhotos()))")
    ListingDto toDto(Listing entity);

    List<ListingDto> toDtoList(List<Listing> entities);

    default List<String> toList(String[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    default String firstOrNull(String[] array) {
        return (array == null || array.length == 0) ? null : array[0];
    }
}