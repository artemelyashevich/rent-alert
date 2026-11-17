package com.rentalert.dto.notification;

import java.time.Instant;

/**
 * Where and when to deliver a notification to a user, resolved by identity-ms.
 *
 * @param receiver the user's encrypted addresses, see {@link NotificationRequest.Receiver}
 * @param sendAt the end of the user's quiet hours when they are on now; null sends right away
 */
public record NotificationDelivery(NotificationRequest.Receiver receiver, Instant sendAt) {

}
