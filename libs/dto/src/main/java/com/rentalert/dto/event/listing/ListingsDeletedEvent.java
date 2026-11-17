package com.rentalert.dto.event.listing;

import com.rentalert.dto.event.KafkaTopics;
import java.util.List;

/**
 * Listings removed by listing-ms, sent to {@link KafkaTopics#LISTINGS_DELETED}. Consumers drop their data tied to these listings; the ids are the same in every service.
 */
public record ListingsDeletedEvent(List<Long> listingIds) {

}
