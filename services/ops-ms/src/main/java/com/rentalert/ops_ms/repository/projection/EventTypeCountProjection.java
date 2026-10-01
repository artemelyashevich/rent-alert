package com.rentalert.ops_ms.repository.projection;

import com.rentalert.dto.event.user.UserEvent;

public interface EventTypeCountProjection {

    UserEvent getEventType();

    long getTotal();
}
