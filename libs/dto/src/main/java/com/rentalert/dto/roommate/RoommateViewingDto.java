package com.rentalert.dto.roommate;

import java.time.Instant;

/**
 * Просмотр пары со снимком данных объявления на момент планирования. Используется напоминаниями в notification-ms.
 */
public record RoommateViewingDto(
    Long id,
    Long matchId,
    Long userLoId,
    Long userHiId,
    Instant viewingDate,
    RoommateViewingStatus status,
    Long listingId,
    String address,
    String listingTitle,
    String listingStreet
) {

}
