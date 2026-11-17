package com.rentalert.roommate_ms.mapper;

import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.dto.RoommateListingResponse;
import java.util.Arrays;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoommateListingMapper {

    RoommateListingResponse toResponse(RoommateListing entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorUserId", ignore = true)
    @Mapping(target = "addressPublic", ignore = true)
    @Mapping(target = "geoLat", ignore = true)
    @Mapping(target = "geoLng", ignore = true)
    @Mapping(target = "descriptionSubmitted", source = "description")
    @Mapping(target = "descriptionApproved", ignore = true)
    @Mapping(target = "descriptionModStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reportsCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget RoommateListing entity, RoommateListingRequest request);

    @AfterMapping
    default void normalizeArrays(@MappingTarget RoommateListing entity) {
        if (entity.getGenderPref() == null || entity.getGenderPref().length == 0) {
            entity.setGenderPref(toStringArray(Gender.values()));
        }
        if (entity.getHouseRules() == null) {
            entity.setHouseRules(new String[0]);
        }
    }

    default String[] toStringArray(Gender[] genders) {
        return genders == null ? new String[0] : Arrays.stream(genders).map(Enum::name).toArray(String[]::new);
    }
}
