package com.rentalert.ops_ms.mapper;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.ops_ms.domain.JobSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface JobSettingMapper {

    @Mapping(target = "eventId", source = "idempotencyKey")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobSetting toEntity(JobExecutionEvent event);

    JobSettingResponse toResponse(JobSetting jobSetting);
}
