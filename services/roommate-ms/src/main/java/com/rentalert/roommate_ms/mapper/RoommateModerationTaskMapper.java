package com.rentalert.roommate_ms.mapper;

import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoommateModerationTaskMapper {

    RoommateModerationTaskResponse toResponse(RoommateModerationTask entity);
}
