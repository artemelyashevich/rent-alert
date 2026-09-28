package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.BroadcastSummaryResponse;
import com.rentalert.notification_ms.domain.NotificationBroadcast;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationBroadcastMapper {

    @Mapping(target = "createdByUserId", source = "createdBy.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "sent", source = "sentCount")
    @Mapping(target = "failed", source = "failedCount")
    @Mapping(target = "skipped", source = "skippedCount")
    BroadcastSummaryResponse toSummary(NotificationBroadcast broadcast);
}
