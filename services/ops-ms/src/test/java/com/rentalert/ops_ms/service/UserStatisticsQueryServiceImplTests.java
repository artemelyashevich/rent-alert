package com.rentalert.ops_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.ops_ms.repository.UserStatisticsRepository;
import com.rentalert.ops_ms.repository.projection.EventTypeCountProjection;
import com.rentalert.ops_ms.repository.projection.UserActivityCountProjection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class UserStatisticsQueryServiceImplTests {

    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    private final UserStatisticsRepository repository = mock(UserStatisticsRepository.class);
    private final UserStatisticsQueryService service = new UserStatisticsQueryServiceImpl(repository, MINSK);

    private static UserActivityCountProjection activity(Long userId, long count) {
        return new UserActivityCountProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public long getActionCount() {
                return count;
            }
        };
    }

    @Test
    void summaryAddsUpActionGroupsAndRate() {
        LocalDate today = LocalDate.now(MINSK);
        LocalDate since = today.minusDays(3);
        when(repository.countInRange(eq(5L), any(), eq(since), eq(today))).thenReturn(0L);
        when(repository.countInRange(5L, Set.of(UserEvent.LOGIN_SUCCESS), since, today)).thenReturn(4L);
        when(repository.countInRange(5L, Set.of(UserEvent.BOT_COMMAND), since, today)).thenReturn(6L);
        when(repository.findLastActivityDate(5L, since)).thenReturn(Optional.of(today.minusDays(1)));

        UserActionSummary summary = service.getUserActionSummary(5L, since);

        assertThat(summary.totalActions()).isEqualTo(10L);
        assertThat(summary.loginCount()).isEqualTo(4L);
        assertThat(summary.botCommandCount()).isEqualTo(6L);
        assertThat(summary.actionRatePerDay()).isEqualTo(2.5);
        assertThat(summary.lastActivityDate()).isEqualTo(today.minusDays(1));
        assertThat(summary.username()).isNull();
    }

    @Test
    void topUsersKeepRepositoryOrder() {
        LocalDate since = LocalDate.now(MINSK).minusDays(7);
        when(repository.findMostActiveUsers(since, PageRequest.of(0, 2))).thenReturn(List.of(activity(7L, 9), activity(3L, 2)));
        when(repository.findLastActivityDate(anyLong(), eq(since))).thenReturn(Optional.empty());

        assertThat(service.getTopActiveUsers(2, since)).extracting(UserActionSummary::userId).containsExactly(7L, 3L);
    }

    @Test
    void overviewCollectsDistributionAndTopUsers() {
        LocalDate today = LocalDate.now(MINSK);
        LocalDate since = today.minusDays(30);
        when(repository.findEventDistribution(eq(since), eq(today), any())).thenReturn(List.of(new EventTypeCountProjection() {
            @Override
            public UserEvent getEventType() {
                return UserEvent.LOGIN_SUCCESS;
            }

            @Override
            public long getTotal() {
                return 12;
            }
        }));
        when(repository.findMostActiveUsers(since, PageRequest.of(0, 10))).thenReturn(List.of(activity(7L, 9)));
        when(repository.countDistinctUsers(today, today)).thenReturn(3L);
        when(repository.countDistinctUsers(today.minusDays(6), today)).thenReturn(8L);
        when(repository.countTotal(UserEvent.FEEDBACK_CREATED, since, today)).thenReturn(2L);

        SystemActivityOverview overview = service.getSystemActivityOverview(since);

        assertThat(overview.totalActiveUsers()).isEqualTo(3L);
        assertThat(overview.totalUsersThisWeek()).isEqualTo(8L);
        assertThat(overview.totalFeedbacks()).isEqualTo(2L);
        assertThat(overview.eventDistribution()).containsEntry(UserEvent.LOGIN_SUCCESS, 12L);
        assertThat(overview.topActiveUsers()).containsExactly(new SystemActivityOverview.MostActiveUser(7L, 9L));
    }
}
