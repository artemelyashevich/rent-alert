package com.rentalert.dto.event;

import com.rentalert.dto.notification.RealtimeNotificationPayload;
import java.util.UUID;

public record RealtimePushEvent(UUID eventId, String userId, RealtimeNotificationPayload payload) {

}
