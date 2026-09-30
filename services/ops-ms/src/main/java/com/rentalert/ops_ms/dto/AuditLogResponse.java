package com.rentalert.ops_ms.dto;

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
