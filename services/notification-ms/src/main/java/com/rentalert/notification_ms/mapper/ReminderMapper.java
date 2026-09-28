package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.ReminderResponseDto;
import com.rentalert.notification_ms.domain.Reminder;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReminderMapper {

    ReminderResponseDto toResponse(Reminder reminder);

    List<ReminderResponseDto> toResponseList(List<Reminder> reminders);
}
