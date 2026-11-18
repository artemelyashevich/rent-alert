package com.rentalert.dto.event.listing;

import java.util.List;

public record ListingsDeletedEvent(List<Long> listingIds) {

}
