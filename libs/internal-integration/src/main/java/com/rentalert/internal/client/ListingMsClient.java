package com.rentalert.internal.client;

import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.dto.listing.FavoriteViewingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = ListingMsClient.SERVICE_NAME, path = "/listing/internal")
public interface ListingMsClient {

    String SERVICE_NAME = "listing-ms";

    @GetMapping("/favorites/{userId}/{listingId}")
    FavoriteViewingDto getFavoriteViewing(@PathVariable("userId") Long userId, @PathVariable("listingId") Long listingId);

    @PostMapping("/favorites/{userId}/{listingId}")
    FavoriteAddResult addFavorite(@PathVariable("userId") Long userId, @PathVariable("listingId") Long listingId);

    @DeleteMapping("/favorites/{userId}/{listingId}")
    boolean removeFavorite(@PathVariable("userId") Long userId, @PathVariable("listingId") Long listingId);

    @PostMapping("/hidden-listings/{userId}/{listingId}")
    boolean hideListing(@PathVariable("userId") Long userId, @PathVariable("listingId") Long listingId);
}
