package com.rentalert.processing_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import com.rentalert.processing_ms.repository.ListingStatisticsRepository;
import com.rentalert.processing_ms.repository.projection.DailyProviderArrivalProjection;
import com.rentalert.processing_ms.repository.projection.ProviderLastParsedProjection;
import com.rentalert.processing_ms.repository.projection.ProviderListingCountProjection;
import com.rentalert.processing_ms.repository.projection.RoomsListingCountProjection;
import com.rentalert.processing_ms.service.impl.ListingStatisticsServiceImpl;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListingStatisticsServiceImplTests {

    private final ListingStatisticsRepository repository = mock(ListingStatisticsRepository.class);
    private final ListingStatisticsService service = new ListingStatisticsServiceImpl(repository);

    private static ProviderListingCountProjection providerCount(ScheduleProvider provider, long total, long active) {
        return new ProviderListingCountProjection() {
            @Override
            public ScheduleProvider getProvider() {
                return provider;
            }

            @Override
            public long getTotal() {
                return total;
            }

            @Override
            public long getActive() {
                return active;
            }
        };
    }

    private static ProviderLastParsedProjection lastParsed(ScheduleProvider provider, Instant at) {
        return new ProviderLastParsedProjection() {
            @Override
            public ScheduleProvider getProvider() {
                return provider;
            }

            @Override
            public Instant getLastParsedAt() {
                return at;
            }
        };
    }

    private static RoomsListingCountProjection rooms(Integer rooms, long total) {
        return new RoomsListingCountProjection() {
            @Override
            public Integer getRooms() {
                return rooms;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static DailyProviderArrivalProjection daily(LocalDate date, ScheduleProvider provider, long total) {
        return new DailyProviderArrivalProjection() {
            @Override
            public int getArrivalYear() {
                return date.getYear();
            }

            @Override
            public int getArrivalMonth() {
                return date.getMonthValue();
            }

            @Override
            public int getArrivalDay() {
                return date.getDayOfMonth();
            }

            @Override
            public ScheduleProvider getProvider() {
                return provider;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void overviewSummarizesProvidersRoomsAndDailyArrivals() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(repository.count()).thenReturn(4L);
        when(repository.countByActive(true)).thenReturn(3L);
        when(repository.countByProvider()).thenReturn(List.of(
            providerCount(ScheduleProvider.REALT, 1, 1), providerCount(ScheduleProvider.KUFAR, 3, 2)));
        when(repository.findLastParsedAtByProvider()).thenReturn(List.of(
            lastParsed(ScheduleProvider.KUFAR, Instant.now().minus(10, ChronoUnit.MINUTES)),
            lastParsed(ScheduleProvider.REALT, Instant.now().minus(7, ChronoUnit.HOURS))));
        when(repository.countByRooms()).thenReturn(List.of(rooms(2, 3), rooms(null, 1)));
        when(repository.countDailyArrivalsByProviderSince(any())).thenReturn(List.of(
            daily(today, ScheduleProvider.KUFAR, 2), daily(today, ScheduleProvider.REALT, 1)));
        when(repository.countNewSince(any())).thenReturn(3L);

        ListingStatisticsOverview overview = service.getOverview(today.minusDays(1));

        assertThat(overview.inactiveListings()).isEqualTo(1L);
        assertThat(overview.byProvider()).extracting(ListingStatisticsOverview.ProviderStat::provider)
            .containsExactly(ScheduleProvider.KUFAR, ScheduleProvider.REALT);
        assertThat(overview.byProvider().getFirst().sharePercent()).isEqualTo(75.0);
        assertThat(overview.staleProviders()).contains(ScheduleProvider.REALT, ScheduleProvider.ONLINER)
            .doesNotContain(ScheduleProvider.KUFAR);
        assertThat(overview.roomsDistribution()).containsEntry("2", 3L).containsEntry("unknown", 1L);
        assertThat(overview.period().days()).isEqualTo(2);
        assertThat(overview.period().newPerDay()).isEqualTo(1.5);
        assertThat(overview.period().dailyArrivals()).singleElement()
            .satisfies(day -> assertThat(day.total()).isEqualTo(3L));
    }
}
