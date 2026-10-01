package com.rentalert.internal.client;

import com.rentalert.dto.listing.FavoriteViewingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = ListingMsClient.SERVICE_NAME, path = "/listing/internal")
public interface ListingMsClient {

    String SERVICE_NAME = "listing-ms";

    @GetMapping("/favorites/{userId}/{listingId}")
    FavoriteViewingDto getFavoriteViewing(@PathVariable("userId") Long userId, @PathVariable("listingId") Long listingId);
}
