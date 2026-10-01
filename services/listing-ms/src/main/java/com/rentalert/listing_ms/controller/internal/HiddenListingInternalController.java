package com.rentalert.listing_ms.controller.internal;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.service.HiddenListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_HIDDEN_LISTING_TAG)
@RequiredArgsConstructor
public class HiddenListingInternalController {

    private final HiddenListingService hiddenListingService;

    @PostMapping("/{userId}/{listingId}")
    public boolean hide(@PathVariable Long userId, @PathVariable Long listingId) {
        try {
            hiddenListingService.hideListing(userId, listingId);
            return true;
        } catch (AppException e) {
            if (e.getErrorCode() == InternalExceptionCode.EX400_HIDDEN_LISTING_ALREADY_EXISTS) {
                return false;
            }
            throw e;
        }
    }
}
