package com.rentalert.roommate_ms.mapper;

import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.dto.RoommateProfileResponse;
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
public interface RoommateProfileMapper {

    RoommateProfileResponse toResponse(RoommateProfile entity);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bioModStatus", ignore = true)
    @Mapping(target = "bioSubmitted", source = "bio")
    @Mapping(target = "bioApproved", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "searchable", ignore = true)
    @Mapping(target = "profileVersion", ignore = true)
    @Mapping(target = "reportsCount", ignore = true)
    @Mapping(target = "lastDigestAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget RoommateProfile entity, RoommateProfileRequest request);

    @AfterMapping
    default void normalizeArrays(@MappingTarget RoommateProfile entity) {
        if (entity.getGenderPref() == null || entity.getGenderPref().length == 0) {
            entity.setGenderPref(toStringArray(Gender.values()));
        }
        if (entity.getLifestyleTags() == null) {
            entity.setLifestyleTags(new String[0]);
        }
    }

    default String[] toStringArray(Gender[] genders) {
        return genders == null ? new String[0] : Arrays.stream(genders).map(Enum::name).toArray(String[]::new);
    }

    default Gender[] toGenderArray(String[] values) {
        return values == null ? new Gender[0] : Arrays.stream(values).map(Gender::valueOf).toArray(Gender[]::new);
    }
}
