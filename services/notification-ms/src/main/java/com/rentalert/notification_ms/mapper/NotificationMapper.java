package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.domain.NotificationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = NotificationStatus.class)
public interface NotificationMapper {

    @Mapping(target = "status", expression = "java(NotificationStatus.SUCCESS)")
    @Mapping(target = "attempt", constant = "1")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "retryType", source = "type")
    Notification toSuccessEntity(NotificationDto dto);


    @Mapping(target = "status", expression = "java(NotificationStatus.FAILURE)")
    @Mapping(target = "attempt", constant = "1")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "retryType", source = "dto.type")
    Notification toFailureEntity(NotificationDto dto, String errorMessage);

    NotificationDto toDto(Notification notification);
}