package com.rentalert.dto.audit;

import java.time.Instant;

public record AuditLogResponse(

    Long id,

    String eventName,

    String eventDescription,

    Long userId,

    String eventInitiator,

    String eventStatus,

    Instant createdAt
) {

}
