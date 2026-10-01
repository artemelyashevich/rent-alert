package com.rentalert.processing_ms.service.impl;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import com.rentalert.dto.statistics.ListingStatisticsOverview.CategoryStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.CityArrival;
import com.rentalert.dto.statistics.ListingStatisticsOverview.CityProviderArrival;
import com.rentalert.dto.statistics.ListingStatisticsOverview.CityProviderStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.CityStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.DailyArrival;
import com.rentalert.dto.statistics.ListingStatisticsOverview.DataQualityStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.PeriodStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.PriceStat;
import com.rentalert.dto.statistics.ListingStatisticsOverview.ProviderArrival;
import com.rentalert.dto.statistics.ListingStatisticsOverview.ProviderStat;
import com.rentalert.processing_ms.repository.ListingStatisticsRepository;
import com.rentalert.processing_ms.repository.projection.DailyProviderArrivalProjection;
import com.rentalert.processing_ms.repository.projection.ProviderLastParsedProjection;
import com.rentalert.processing_ms.service.ListingStatisticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingStatisticsServiceImpl implements ListingStatisticsService {

    private static final long STALE_THRESHOLD_HOURS = 6;

    private final ListingStatisticsRepository repository;

    private static BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private static Double percent(long part, long whole) {
        if (whole == 0) {
            return 0.0;
        }
        return Math.round((double) part / whole * 10_000.0) / 100.0;
    }

    private static Double perDay(long count, long days) {
        return Math.round((double) count / days * 100.0) / 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public ListingStatisticsOverview getOverview(LocalDate since) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant sinceInstant = since.atStartOfDay(ZoneOffset.UTC).toInstant();
        long days = Math.max(1, ChronoUnit.DAYS.between(since, today) + 1);

        long total = repository.count();
        long active = repository.countByActive(true);
        Map<ScheduleProvider, Instant> lastParsed = readLastParsedAtByProvider();

        return ListingStatisticsOverview.builder()
            .totalListings(total)
            .activeListings(active)
            .inactiveListings(total - active)
            .byProvider(buildProviderStats(total, lastParsed, now))
            .byCategory(repository.countByCategory().stream()
                .map(row -> new CategoryStat(row.getCategory(), row.getTotal(), row.getActive()))
                .toList())
            .byCity(repository.countByCity().stream()
                .map(row -> new CityStat(row.getCity(), row.getTotal(), row.getActive()))
                .toList())
            .byCityAndProvider(repository.countByCityAndProvider().stream()
                .map(row -> new CityProviderStat(row.getCity(), row.getProvider(), row.getTotal()))
                .toList())
            .period(buildPeriod(since, today, days, sinceInstant))
            .newLast24h(repository.countNewSince(now.minus(24, ChronoUnit.HOURS)))
            .newLast7d(repository.countNewSince(now.minus(7, ChronoUnit.DAYS)))
            .newLast30d(repository.countNewSince(now.minus(30, ChronoUnit.DAYS)))
            .staleProviders(findStaleProviders(lastParsed, now))
            .priceStats(repository.findPriceStatsByCurrency().stream()
                .map(row -> new PriceStat(row.getCurrency(), row.getTotal(),
                    toBigDecimal(row.getAvgPrice()), toBigDecimal(row.getMinPrice()), toBigDecimal(row.getMaxPrice())))
                .toList())
            .roomsDistribution(buildRoomsDistribution())
            .dataQuality(new DataQualityStat(repository.countByCityIsNull(), repository.countByPriceIsNull(),
                repository.countWithoutPhotos()))
            .build();
    }

    private List<ProviderStat> buildProviderStats(long total, Map<ScheduleProvider, Instant> lastParsed, Instant now) {
        return repository.countByProvider().stream()
            .map(row -> {
                Instant last = lastParsed.get(row.getProvider());
                return new ProviderStat(row.getProvider(), row.getTotal(), row.getActive(), row.getTotal() - row.getActive(),
                    percent(row.getTotal(), total), last, last == null ? null : Duration.between(last, now).toMinutes());
            })
            .sorted(Comparator.comparing(ProviderStat::total).reversed())
            .toList();
    }

    private PeriodStat buildPeriod(LocalDate since, LocalDate until, long days, Instant sinceInstant) {
        long newTotal = repository.countNewSince(sinceInstant);
        return PeriodStat.builder()
            .since(since)
            .until(until)
            .days(days)
            .newTotal(newTotal)
            .newPerDay(perDay(newTotal, days))
            .newByProvider(repository.countNewByProviderSince(sinceInstant).stream()
                .map(row -> new ProviderArrival(row.getProvider(), row.getTotal(), perDay(row.getTotal(), days)))
                .toList())
            .newByCity(repository.countNewByCitySince(sinceInstant).stream()
                .map(row -> new CityArrival(row.getCity(), row.getTotal(), perDay(row.getTotal(), days)))
                .toList())
            .newByCityAndProvider(repository.countNewByCityAndProviderSince(sinceInstant).stream()
                .map(row -> new CityProviderArrival(row.getCity(), row.getProvider(), row.getTotal(), perDay(row.getTotal(), days)))
                .toList())
            .dailyArrivals(buildDailyArrivals(sinceInstant))
            .build();
    }

    private List<DailyArrival> buildDailyArrivals(Instant sinceInstant) {
        Map<LocalDate, Map<ScheduleProvider, Long>> byDate = new LinkedHashMap<>();
        for (DailyProviderArrivalProjection row : repository.countDailyArrivalsByProviderSince(sinceInstant)) {
            LocalDate date = LocalDate.of(row.getArrivalYear(), row.getArrivalMonth(), row.getArrivalDay());
            byDate.computeIfAbsent(date, d -> new EnumMap<>(ScheduleProvider.class)).put(row.getProvider(), row.getTotal());
        }
        List<DailyArrival> result = new ArrayList<>();
        byDate.forEach((date, byProvider) -> result.add(
            new DailyArrival(date, byProvider.values().stream().mapToLong(Long::longValue).sum(), byProvider)));
        return result;
    }

    private Map<String, Long> buildRoomsDistribution() {
        Map<String, Long> result = new LinkedHashMap<>();
        repository.countByRooms().forEach(row ->
            result.merge(row.getRooms() == null ? "unknown" : String.valueOf(row.getRooms()), row.getTotal(), Long::sum));
        return result;
    }

    private Map<ScheduleProvider, Instant> readLastParsedAtByProvider() {
        Map<ScheduleProvider, Instant> result = new EnumMap<>(ScheduleProvider.class);
        for (ProviderLastParsedProjection row : repository.findLastParsedAtByProvider()) {
            if (row.getLastParsedAt() != null) {
                result.put(row.getProvider(), row.getLastParsedAt());
            }
        }
        return result;
    }

    private List<ScheduleProvider> findStaleProviders(Map<ScheduleProvider, Instant> lastParsed, Instant now) {
        List<ScheduleProvider> stale = new ArrayList<>();
        for (ScheduleProvider provider : ScheduleProvider.values()) {
            Instant last = lastParsed.get(provider);
            if (last == null || Duration.between(last, now).toHours() >= STALE_THRESHOLD_HOURS) {
                stale.add(provider);
            }
        }
        return stale;
    }
}
