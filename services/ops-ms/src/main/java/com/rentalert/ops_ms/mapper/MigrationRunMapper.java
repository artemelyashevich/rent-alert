package com.rentalert.ops_ms.mapper;

import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationStepResponse;
import com.rentalert.ops_ms.domain.MigrationRun;
import com.rentalert.ops_ms.domain.MigrationRunStep;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MigrationRunMapper {

    @Mapping(target = "step", source = "stepOrder")
    MigrationStepResponse toStepResponse(MigrationRunStep step);

    @Mapping(target = "steps", source = "steps")
    MigrationRunResponse toResponse(MigrationRun run, List<MigrationRunStep> steps);
}
