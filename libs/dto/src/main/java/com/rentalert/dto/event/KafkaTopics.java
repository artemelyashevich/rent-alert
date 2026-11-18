package com.rentalert.dto.event;

import com.rentalert.dto.notification.NotificationType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaTopics {

    public static final String NOTIFICATION_REQUESTS = "notification.requests.v1";

    public static final String NOTIFICATION_REQUESTS_PRIORITY = "notification.requests.priority.v1";

    public static final String AUDIT_EVENTS = "ops.audit-events.v1";

    public static final String USER_EVENTS = "ops.user-events.v1";

    public static final String JOB_EVENTS = "ops.job-events.v1";

    public static final String CLEANUP_COMMANDS = "ops.cleanup-commands.v1";

    public static final String CLEANUP_REPLIES = "ops.cleanup-replies.v1";

    public static final String MIGRATION_COMMANDS = "ops.migration-commands.v1";

    public static final String MIGRATION_PROGRESS = "ops.migration-progress.v1";

    public static final String MIGRATION_REPLIES = "ops.migration-replies.v1";

    /** {@link ServiceStartedEvent} from every service with a Kafka producer, key = service name. */
    public static final String SERVICE_STARTED = "ops.service-started.v1";

    /** {@code ListingCreatedEvent} from processing-ms, key = listingId. */
    public static final String LISTINGS = "processing.listings.v1";

    public static final String LISTINGS_DELETED = "listing.listings-deleted.v1";

    public static final String REALTIME_PUSH = "notification.realtime-push.v1";

    public static final String ROOMMATE_COMPLAINTS = "identity.roommate-complaints.v1";

    public static String notificationRequests(NotificationType type) {
        return type.isPriority()
            ? NOTIFICATION_REQUESTS_PRIORITY
            : NOTIFICATION_REQUESTS;
    }
}
