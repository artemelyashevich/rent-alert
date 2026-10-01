package com.rentalert.processing_ms.repository;

import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.repository.projection.CategoryListingCountProjection;
import com.rentalert.processing_ms.repository.projection.CityArrivalProjection;
import com.rentalert.processing_ms.repository.projection.CityListingCountProjection;
import com.rentalert.processing_ms.repository.projection.CityProviderCountProjection;
import com.rentalert.processing_ms.repository.projection.CurrencyPriceStatProjection;
import com.rentalert.processing_ms.repository.projection.DailyProviderArrivalProjection;
import com.rentalert.processing_ms.repository.projection.ProviderArrivalProjection;
import com.rentalert.processing_ms.repository.projection.ProviderLastParsedProjection;
import com.rentalert.processing_ms.repository.projection.ProviderListingCountProjection;
import com.rentalert.processing_ms.repository.projection.RoomsListingCountProjection;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ListingStatisticsRepository extends Repository<@NonNull Listing, @NonNull Long> {

    long count();

    long countByActive(boolean active);

    long countByCityIsNull();

    long countByPriceIsNull();

    @Query(value = "select count(*) from listings where photos is null or array_length(photos, 1) is null", nativeQuery = true)
    long countWithoutPhotos();

    @Query("""
        select l.source as provider, count(l) as total, sum(case when l.active = true then 1L else 0L end) as active
        from Listing l
        group by l.source
        """)
    List<ProviderListingCountProjection> countByProvider();

    @Query("""
        select l.city as city, count(l) as total, sum(case when l.active = true then 1L else 0L end) as active
        from Listing l
        group by l.city
        order by count(l) desc
        """)
    List<CityListingCountProjection> countByCity();

    @Query("""
        select l.category as category, count(l) as total, sum(case when l.active = true then 1L else 0L end) as active
        from Listing l
        group by l.category
        """)
    List<CategoryListingCountProjection> countByCategory();

    @Query("""
        select l.city as city, l.source as provider, count(l) as total
        from Listing l
        group by l.city, l.source
        order by count(l) desc
        """)
    List<CityProviderCountProjection> countByCityAndProvider();

    @Query("select l.source as provider, max(l.parsedAt) as lastParsedAt from Listing l group by l.source")
    List<ProviderLastParsedProjection> findLastParsedAtByProvider();

    @Query("select count(l) from Listing l where l.parsedAt >= :since")
    long countNewSince(@Param("since") Instant since);

    @Query("""
        select l.source as provider, count(l) as total
        from Listing l
        where l.parsedAt >= :since
        group by l.source
        """)
    List<ProviderArrivalProjection> countNewByProviderSince(@Param("since") Instant since);

    @Query("""
        select l.city as city, count(l) as total
        from Listing l
        where l.parsedAt >= :since
        group by l.city
        order by count(l) desc
        """)
    List<CityArrivalProjection> countNewByCitySince(@Param("since") Instant since);

    @Query("""
        select l.city as city, l.source as provider, count(l) as total
        from Listing l
        where l.parsedAt >= :since
        group by l.city, l.source
        order by count(l) desc
        """)
    List<CityProviderCountProjection> countNewByCityAndProviderSince(@Param("since") Instant since);

    @Query("""
        select year(l.parsedAt) as arrivalYear, month(l.parsedAt) as arrivalMonth, day(l.parsedAt) as arrivalDay,
               l.source as provider, count(l) as total
        from Listing l
        where l.parsedAt >= :since
        group by year(l.parsedAt), month(l.parsedAt), day(l.parsedAt), l.source
        order by year(l.parsedAt), month(l.parsedAt), day(l.parsedAt)
        """)
    List<DailyProviderArrivalProjection> countDailyArrivalsByProviderSince(@Param("since") Instant since);

    @Query("""
        select l.currency as currency, count(l) as total, avg(l.price) as avgPrice, min(l.price) as minPrice, max(l.price) as maxPrice
        from Listing l
        where l.price is not null
        group by l.currency
        """)
    List<CurrencyPriceStatProjection> findPriceStatsByCurrency();

    @Query("select l.rooms as rooms, count(l) as total from Listing l group by l.rooms")
    List<RoomsListingCountProjection> countByRooms();
}
