package com.rentalert.listing_ms.service;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.listing_ms.domain.Listing;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {

    Page<ListingDto> findAll(String filter, Set<String> sources, ListingCategory category, Pageable pageable);

    /** Объявление по id. Бросает 404, если не найдено. */
    Listing getById(Long id);

    List<ListingDto> findCards(Collection<Long> ids);

    /**
     * Идентификаторы объявлений, которые можно безопасно удалить: не парсились дольше {@code cutoff} и ни у кого не в избранном / не скрыты. Ограничено {@code limit}.
     */
    List<Long> findStaleListingIds(Instant cutoff, int limit);

    /** Удаляет объявления с указанными id. Возвращает число удалённых. */
    int deleteByIds(List<Long> listingIds);

    /** Сохраняет объявление, полученное от processing-ms; повторная доставка игнорируется. */
    void saveCreated(ListingCreatedEvent event);
}
