package com.rentalert.notification_ms.mapper;

import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.notification_ms.domain.NotificationBroadcastRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationBroadcastRecipientMapper {

    @Mapping(target = "userId", source = "userProfile.id")
    @Mapping(target = "username", source = "userProfile.username")
    @Mapping(target = "photoUrl", source = "userProfile.photoUrl")
    BroadcastRecipientResponse toResponse(NotificationBroadcastRecipient recipient);
}
