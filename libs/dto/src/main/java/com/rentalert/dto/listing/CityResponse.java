package com.rentalert.dto.listing;


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
