package com.rentalert.roommate_ms.service;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.redis.dto.CacheType;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateListingCards {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final ListingMsService listingMsService;
    private final RoommateBatchCache batchCache;

    public Map<Long, ListingDto> find(Collection<Long> listingIds) {
        return batchCache.getAll(CacheType.LISTING_CARD, listingIds, TTL, missing -> listingMsService.findCards(missing).stream()
            .collect(Collectors.toMap(ListingDto::getId, Function.identity(), (a, b) -> a)));
    }

    public void evict(Collection<Long> listingIds) {
        batchCache.evictAll(CacheType.LISTING_CARD, listingIds);
    }
}
