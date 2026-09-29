package com.rentalert.core_ms.mapper;

import com.rentalert.core_ms.domain.SchedulerSettings;
import com.rentalert.core_ms.dto.SchedulerSettingsRequest;
import com.rentalert.core_ms.dto.SchedulerSettingsResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SchedulerSettingsMapper {

    SchedulerSettingsResponse toResponse(SchedulerSettings schedulerSettings);

    List<SchedulerSettingsResponse> toResponse(List<SchedulerSettings> schedulerSettings);

    @Mapping(target = "providerName", source = "request.providerName", qualifiedByName = "mapSchedulerProvider")
    SchedulerSettings toEntity(SchedulerSettingsRequest request);

    @Mapping(target = "settings.providerName" ,source = "request.providerName", qualifiedByName = "mapSchedulerProvider")
    void update(@MappingTarget SchedulerSettings settings, SchedulerSettingsRequest request);

    @Named("mapSchedulerProvider")
    default com.rentalert.dto.listing.ScheduleProvider mapSchedulerProvider(String providerName) {
        return com.rentalert.dto.listing.ScheduleProvider.getScheduleProvider(providerName);
    }
}
