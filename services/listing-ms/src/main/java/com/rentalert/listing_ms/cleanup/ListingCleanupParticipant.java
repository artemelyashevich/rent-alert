package com.rentalert.listing_ms.cleanup;

import com.rentalert.common.util.BatchDeleter;
import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import com.rentalert.listing_ms.messaging.ListingEventPublisher;
import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.HiddenListingRepository;
import com.rentalert.listing_ms.repository.ListingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.ToIntFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class ListingCleanupParticipant extends AbstractCleanupParticipant {

    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;
    private final HiddenListingRepository hiddenListingRepository;
    private final ListingEventPublisher listingEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Duration listingRetention;
    private final int maxRowsPerRun;

    public ListingCleanupParticipant(@Value("${spring.application.name}") String serviceName, ListingRepository listingRepository, FavoriteRepository favoriteRepository, HiddenListingRepository hiddenListingRepository,
                                     ListingEventPublisher listingEventPublisher, TransactionTemplate transactionTemplate,
                                     @Value("${app.cleanup.listing-retention:P7D}") Duration listingRetention, @Value("${app.cleanup.max-rows-per-run:20000}") int maxRowsPerRun) {
        super(serviceName);
        this.listingRepository = listingRepository;
        this.favoriteRepository = favoriteRepository;
        this.hiddenListingRepository = hiddenListingRepository;
        this.listingEventPublisher = listingEventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.listingRetention = listingRetention;
        this.maxRowsPerRun = maxRowsPerRun;
    }

    @Override
    public CleanupResult cleanUp(Instant referenceTime) {
        long startedAt = System.nanoTime();
        List<Long> staleIds;
        try {
            staleIds = listingRepository.findStaleListingIds(referenceTime.minus(listingRetention), PageRequest.of(0, maxRowsPerRun));
        } catch (RuntimeException e) {
            log.error("[CLEANUP] listings: failed to select stale listings", e);
            return CleanupResult.sectionFailed("listings", Duration.ofNanos(System.nanoTime() - startedAt), e);
        }
        return new CleanupResult(0, 0, Duration.ofNanos(System.nanoTime() - startedAt), List.of())
            .plus(deleteFavorites(staleIds))
            .plus(deleteHiddenListings(staleIds))
            .plus(deleteListings(staleIds));
    }

    public CleanupResult deleteFavorites(List<Long> listingIds) {
        return BatchDeleter.delete("favorites", () -> listingIds, inTransaction(favoriteRepository::deleteByListingIdIn));
    }

    public CleanupResult deleteHiddenListings(List<Long> listingIds) {
        return BatchDeleter.delete("hidden-listings", () -> listingIds, inTransaction(hiddenListingRepository::deleteByListingIdIn));
    }

    public CleanupResult deleteListings(List<Long> listingIds) {
        return BatchDeleter.delete("listings", () -> listingIds, batch -> transactionTemplate.execute(status -> {
            int deleted = listingRepository.deleteByIdIn(batch);
            listingEventPublisher.publishListingsDeleted(batch);
            return deleted;
        }));
    }

    private ToIntFunction<List<Long>> inTransaction(ToIntFunction<List<Long>> delete) {
        return batch -> transactionTemplate.execute(status -> delete.applyAsInt(batch));
    }
}
