package com.rentalert.dto.event.listing;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * A new listing parsed by processing-ms, sent to {@link KafkaTopics#LISTINGS} with key = listingId.
 * {@code listingId} is the id assigned by processing-ms; consumers keep it, so the id is the same in every service.
 * {@code priceUsd}, {@code priceByn} and {@code message} (the ready notification text) are null when processing-ms
 * couldn't build them, e.g. without an exchange rate.
 */
@Builder
public record ListingCreatedEvent(
    Long listingId,
    ScheduleProvider source,
    ListingCategory category,
    String sourceId,
    String sourceUrl,
    String title,
    String description,
    String street,
    String metroStation,
    String city,
    BigDecimal price,
    Currency currency,
    Integer rooms,
    Float areaTotal,
    Float roomArea,
    Double geoLat,
    Double geoLng,
    Integer floor,
    Integer floorsTotal,
    boolean furniture,
    List<String> photos,
    Instant parsedAt,
    Instant updatedAtProvider,
    BigDecimal priceUsd,
    BigDecimal priceByn,
    String message
) {
}
