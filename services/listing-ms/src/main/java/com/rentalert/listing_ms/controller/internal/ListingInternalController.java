package com.rentalert.listing_ms.controller.internal;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.service.ListingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_LISTING_TAG)
@RequiredArgsConstructor
public class ListingInternalController {

    private final ListingService listingService;

    /**
     * Карточки объявлений по списку id для других сервисов. Удалённые объявления в ответ не попадают, поэтому
     * вызывающий сравнивает id ответа с запрошенными.
     */
    @PostMapping("/cards")
    public List<ListingDto> findCards(@RequestBody List<Long> ids) {
        return listingService.findCards(ids);
    }
}
