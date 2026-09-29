package com.rentalert.dto.event;

import com.rentalert.dto.notification.NotificationType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaTopics {

    public static final String NOTIFICATION_REQUESTS = "notification.requests.v1";

    public static final String NOTIFICATION_REQUESTS_PRIORITY = "notification.requests.priority.v1";

    public static final String AUDIT_EVENTS = "ops.audit-events.v1";

    public static String notificationRequests(NotificationType type) {
        return type.isPriority()
            ? NOTIFICATION_REQUESTS_PRIORITY
            : NOTIFICATION_REQUESTS;
    }
}
