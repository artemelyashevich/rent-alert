package com.rentalert.processing_ms.dto;


import com.rentalert.dto.listing.ScheduleProvider;
import java.time.Instant;

public record CityResponse(
    Long id,
    ScheduleProvider provider,
    String name,
    String cityCode,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {

}
