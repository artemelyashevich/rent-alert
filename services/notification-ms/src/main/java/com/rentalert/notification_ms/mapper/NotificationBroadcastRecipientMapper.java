package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.notification_ms.domain.NotificationBroadcastRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationBroadcastRecipientMapper {

    @Mapping(target = "username", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    BroadcastRecipientResponse toResponse(NotificationBroadcastRecipient recipient);
}
