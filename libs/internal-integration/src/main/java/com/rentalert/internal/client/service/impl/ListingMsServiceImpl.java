package com.rentalert.internal.client.service.impl;

import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.ListingMsClient;
import com.rentalert.internal.client.service.ListingMsService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.Optional;

public class ListingMsServiceImpl extends AbstractResilientClient implements ListingMsService {

    private final ListingMsClient listingMsClient;

    public ListingMsServiceImpl(ListingMsClient listingMsClient, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        super(ListingMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.listingMsClient = listingMsClient;
    }

    @Override
    public Optional<FavoriteViewingDto> getFavoriteViewing(Long userId, Long listingId) {
        return Optional.ofNullable(execute(() -> listingMsClient.getFavoriteViewing(userId, listingId)));
    }
}
