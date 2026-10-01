package com.rentalert.ops_ms.service;

import static com.rentalert.dto.event.user.UserEvent.BOT_COMMAND;
import static com.rentalert.dto.event.user.UserEvent.CALLBACK_FAVORITE;
import static com.rentalert.dto.event.user.UserEvent.CALLBACK_HIDE;
import static com.rentalert.dto.event.user.UserEvent.CALLBACK_UNFAVORITE;
import static com.rentalert.dto.event.user.UserEvent.FAVORITE_ADDED;
import static com.rentalert.dto.event.user.UserEvent.FAVORITE_REMOVED;
import static com.rentalert.dto.event.user.UserEvent.FEEDBACK_CREATED;
import static com.rentalert.dto.event.user.UserEvent.FILTER_ACTIVATED;
import static com.rentalert.dto.event.user.UserEvent.FILTER_CREATED;
import static com.rentalert.dto.event.user.UserEvent.FILTER_DELETED;
import static com.rentalert.dto.event.user.UserEvent.FILTER_UPDATED;
import static com.rentalert.dto.event.user.UserEvent.LISTING_RECEIVED;
import static com.rentalert.dto.event.user.UserEvent.LOGIN_FAILED;
import static com.rentalert.dto.event.user.UserEvent.LOGIN_SUCCESS;
import static com.rentalert.dto.event.user.UserEvent.NOTIFICATION_SETTINGS_UPDATED;
import static com.rentalert.dto.event.user.UserEvent.PROFILE_UPDATED;
import static com.rentalert.dto.event.user.UserEvent.TELEGRAM_CALLBACK;

import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import com.rentalert.ops_ms.repository.UserStatisticsRepository;
import com.rentalert.ops_ms.repository.projection.EventTypeCountProjection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserStatisticsQueryServiceImpl implements UserStatisticsQueryService {

    private static final int TOP_USERS_IN_OVERVIEW = 10;
    private static final int LAST_ACTIVITY_DAYS = 30;

    private static final Set<UserEvent> FAVORITE_ACTIONS = Set.of(FAVORITE_ADDED, FAVORITE_REMOVED, CALLBACK_FAVORITE, CALLBACK_UNFAVORITE);
    private static final Set<UserEvent> FILTER_ACTIONS = Set.of(FILTER_CREATED, FILTER_DELETED, FILTER_ACTIVATED, FILTER_UPDATED);
    private static final Set<UserEvent> CALLBACK_ACTIONS = Set.of(TELEGRAM_CALLBACK, CALLBACK_FAVORITE, CALLBACK_UNFAVORITE, CALLBACK_HIDE);
    private static final Set<UserEvent> OVERVIEW_EVENTS = Set.of(LOGIN_SUCCESS, LOGIN_FAILED, PROFILE_UPDATED, FILTER_CREATED,
        FILTER_DELETED, BOT_COMMAND, FEEDBACK_CREATED, TELEGRAM_CALLBACK, CALLBACK_FAVORITE, CALLBACK_UNFAVORITE, CALLBACK_HIDE);

    private final UserStatisticsRepository repository;
    private final ZoneId zone;

    public UserStatisticsQueryServiceImpl(UserStatisticsRepository repository, @Value("${app.bot.timezone:Europe/Minsk}") ZoneId zone) {
        this.repository = repository;
        this.zone = zone;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Override
    public UserStatisticsResponse getUserStatistics(Long userId, Long filterId) {
        LocalDate today = LocalDate.now(zone);
        long favoritesAdded = repository.countWithoutFilter(userId, FAVORITE_ADDED, today);
        long favoritesRemoved = repository.countWithoutFilter(userId, FAVORITE_REMOVED, today);
        long filterUpdates = repository.countWithFilter(userId, FILTER_UPDATED, today);
        long logins = repository.countWithoutFilter(userId, LOGIN_SUCCESS, today);
        long profileUpdates = repository.countWithoutFilter(userId, PROFILE_UPDATED, today);
        long settingsUpdates = repository.countWithoutFilter(userId, NOTIFICATION_SETTINGS_UPDATED, today);
        long botCommands = repository.countWithoutFilter(userId, BOT_COMMAND, today);
        long feedbacks = repository.countWithoutFilter(userId, FEEDBACK_CREATED, today);

        return UserStatisticsResponse.builder()
            .listingsReceivedToday(repository.countWithFilter(userId, LISTING_RECEIVED, today))
            .favoritesAddedToday(favoritesAdded)
            .favoritesRemovedToday(favoritesRemoved)
            .filterUpdatesToday(filterUpdates)
            .averageListingsPerWeek(filterId == null
                ? 0.0
                : repository.averageByFilter(userId, filterId, LISTING_RECEIVED, today.minusDays(6), today))
            .loginCountToday(logins)
            .profileUpdatesToday(profileUpdates)
            .notificationsSettingsUpdatesToday(settingsUpdates)
            .botCommandsToday(botCommands)
            .feedbackCountToday(feedbacks)
            .totalActionsToday(logins + profileUpdates + settingsUpdates + botCommands + feedbacks
                + favoritesAdded + favoritesRemoved + filterUpdates)
            .lastActivityDate(repository.findLastActivityDate(userId, today.minusDays(LAST_ACTIVITY_DAYS)).orElse(null))
            .build();
    }

    @Override
    public UserActionSummary getUserActionSummary(Long userId, LocalDate since) {
        LocalDate today = LocalDate.now(zone);
        long days = Math.max(1, ChronoUnit.DAYS.between(since, today) + 1);

        long logins = repository.countInRange(userId, Set.of(LOGIN_SUCCESS), since, today);
        long favoriteActions = repository.countInRange(userId, FAVORITE_ACTIONS, since, today);
        long filterActions = repository.countInRange(userId, FILTER_ACTIONS, since, today);
        long botCommands = repository.countInRange(userId, Set.of(BOT_COMMAND), since, today);
        long feedbacks = repository.countInRange(userId, Set.of(FEEDBACK_CREATED), since, today);
        long callbacks = repository.countInRange(userId, CALLBACK_ACTIONS, since, today);
        long total = logins + favoriteActions + filterActions + botCommands + feedbacks + callbacks;

        return UserActionSummary.builder()
            .userId(userId)
            .lastActivityDate(repository.findLastActivityDate(userId, since).orElse(since))
            .totalActions(total)
            .loginCount(logins)
            .favoriteActions(favoriteActions)
            .filterActions(filterActions)
            .botCommandCount(botCommands)
            .feedbackCount(feedbacks)
            .callbackCount(callbacks)
            .actionRatePerDay(round2((double) total / days))
            .build();
    }

    @Override
    public List<UserActionSummary> getTopActiveUsers(int limit, LocalDate since) {
        return repository.findMostActiveUsers(since, PageRequest.of(0, limit)).stream()
            .map(row -> getUserActionSummary(row.getUserId(), since))
            .toList();
    }

    @Override
    public List<UserActivityLog> getUserActivityLog(Long userId, LocalDate since, LocalDate until) {
        LocalDate effectiveUntil = until != null ? until : LocalDate.now(zone);
        return repository.findActivity(userId, since, effectiveUntil).stream()
            .map(stats -> new UserActivityLog(stats.getEventType(), stats.getEventDate(), stats.getCount()))
            .toList();
    }

    @Override
    public SystemActivityOverview getSystemActivityOverview(LocalDate since) {
        LocalDate today = LocalDate.now(zone);

        Map<UserEvent, Long> distribution = new EnumMap<>(UserEvent.class);
        for (EventTypeCountProjection row : repository.findEventDistribution(since, today, OVERVIEW_EVENTS)) {
            distribution.put(row.getEventType(), row.getTotal());
        }

        List<SystemActivityOverview.MostActiveUser> topUsers = repository
            .findMostActiveUsers(since, PageRequest.of(0, TOP_USERS_IN_OVERVIEW)).stream()
            .map(row -> new SystemActivityOverview.MostActiveUser(row.getUserId(), row.getActionCount()))
            .toList();

        return SystemActivityOverview.builder()
            .totalActiveUsers(repository.countDistinctUsers(today, today))
            .totalUsersThisWeek(repository.countDistinctUsers(today.minusDays(6), today))
            .totalFeedbacks(repository.countTotal(FEEDBACK_CREATED, since, today))
            .eventDistribution(distribution)
            .topActiveUsers(topUsers)
            .build();
    }
}
