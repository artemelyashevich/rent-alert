package com.rentalert.dto.notification;

import java.time.Instant;

public record NotificationDelivery(NotificationRequest.Receiver receiver, Instant sendAt) {

}
