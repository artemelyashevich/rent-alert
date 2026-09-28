package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationChannelMapper {

    @Mapping(target = "engine", source = "engine")
    @Mapping(target = "enabled", expression = "java(setting != null && setting.isEnabled())")
    @Mapping(target = "isDefault", expression = "java(setting != null && setting.isDefault())")
    @Mapping(target = "available", expression = "java(engine.isEnabled())")
    NotificationChannelDto toDto(NotificationEngine engine, UserNotificationSetting setting);
}