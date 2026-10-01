package com.rentalert.dto.statistics;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record ListingStatisticsOverview(
    Long totalListings,
    Long activeListings,
    Long inactiveListings,
    List<ProviderStat> byProvider,
    List<CategoryStat> byCategory,
    List<CityStat> byCity,
    List<CityProviderStat> byCityAndProvider,
    PeriodStat period,
    Long newLast24h,
    Long newLast7d,
    Long newLast30d,
    List<ScheduleProvider> staleProviders,
    List<PriceStat> priceStats,
    Map<String, Long> roomsDistribution,
    DataQualityStat dataQuality
) {

    public record ProviderStat(ScheduleProvider provider, Long total, Long active, Long inactive, Double sharePercent,
                               Instant lastParsedAt, Long minutesSinceLastParsed) {

    }

    public record CategoryStat(ListingCategory category, Long total, Long active) {

    }

    public record CityStat(String city, Long total, Long active) {

    }

    public record CityProviderStat(String city, ScheduleProvider provider, Long total) {

    }

    @Builder
    public record PeriodStat(LocalDate since, LocalDate until, long days, Long newTotal, Double newPerDay,
                             List<ProviderArrival> newByProvider, List<CityArrival> newByCity,
                             List<CityProviderArrival> newByCityAndProvider, List<DailyArrival> dailyArrivals) {

    }

    public record ProviderArrival(ScheduleProvider provider, Long count, Double perDay) {

    }

    public record CityArrival(String city, Long count, Double perDay) {

    }

    public record CityProviderArrival(String city, ScheduleProvider provider, Long count, Double perDay) {

    }

    public record DailyArrival(LocalDate date, Long total, Map<ScheduleProvider, Long> byProvider) {

    }

    public record PriceStat(Currency currency, Long count, BigDecimal avgPrice, BigDecimal minPrice, BigDecimal maxPrice) {

    }

    public record DataQualityStat(Long withoutCity, Long withoutPrice, Long withoutPhotos) {

    }
}
