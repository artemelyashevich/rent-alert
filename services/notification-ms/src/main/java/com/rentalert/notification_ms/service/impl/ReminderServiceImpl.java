package com.rentalert.notification_ms.service.impl;

import com.bsu.renalert.identity.application.service.UserProfileService;
import com.bsu.renalert.identity.domain.UserProfile;
import com.bsu.renalert.infra.service.CryptoService;
import com.bsu.renalert.listing.application.service.FavoriteService;
import com.bsu.renalert.listing.domain.Favorite;
import com.bsu.renalert.listing.domain.FavoriteEmbeddedId;
import com.bsu.renalert.listing.domain.Listing;
import com.bsu.renalert.listing.infrastructure.mapper.ParserResponseMapper;
import com.bsu.renalert.listing.infrastructure.util.ParserMessageUtil;
import com.bsu.renalert.notification.application.service.NotificationService;
import com.bsu.renalert.notification.application.service.ReminderPersistenceService;
import com.bsu.renalert.notification.application.service.ReminderService;
import com.bsu.renalert.notification.domain.NotificationType;
import com.bsu.renalert.notification.domain.Reminder;
import com.bsu.renalert.notification.dto.NotificationDto;
import com.bsu.renalert.notification.dto.NotificationEngine;
import com.bsu.renalert.notification.dto.ReminderIntervalType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private static final int BATCH_SIZE = 100;

    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter VIEWING_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU_LOCALE);
    private static final String LINK_LABEL = "Смотреть объявление →";

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;
    private final ReminderPersistenceService reminderPersistenceService;
    private final FavoriteService favoriteService;
    private final CryptoService cryptoService;
    private final ParserResponseMapper parserResponseMapper;

    @Value("${app.bot.timezone:Europe/Minsk}")
    private ZoneId botZone;

    @Override
    public void send() {
        List<Reminder> reminders = reminderPersistenceService.findBatchToRemind(BATCH_SIZE);

        if (reminders.isEmpty()) {
            return;
        }

        log.info("Processing {} reminders", reminders.size());

        Map<FavoriteEmbeddedId, Favorite> favoritesById = loadFavorites(reminders);
        Map<Long, UserProfile> usersById = loadUsers(reminders);

        int sent = 0;
        int failed = 0;
        for (Reminder reminder : reminders) {
            try {
                send(reminder, favoritesById, usersById);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to process reminder id={}, userId={}, listingId={}: {}",
                        reminder.getId(), reminder.getUserId(), reminder.getListingId(), e.getMessage(), e);
                reminderPersistenceService.markAsFailed(reminder, e.getMessage());
            }
        }

        log.info("Reminders processed: total={}, sent={}, failed={}", reminders.size(), sent, failed);
    }

    private Map<FavoriteEmbeddedId, Favorite> loadFavorites(List<Reminder> reminders) {
        List<FavoriteEmbeddedId> ids = reminders.stream()
                .map(reminder -> new FavoriteEmbeddedId(reminder.getUserId(), reminder.getListingId()))
                .distinct()
                .toList();
        return favoriteService.findAllByIds(ids).stream()
                .collect(Collectors.toMap(Favorite::getId, Function.identity()));
    }

    private Map<Long, UserProfile> loadUsers(List<Reminder> reminders) {
        List<Long> userIds = reminders.stream()
                .map(Reminder::getUserId)
                .distinct()
                .toList();
        return userProfileService.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
    }

    private void send(Reminder reminder, Map<FavoriteEmbeddedId, Favorite> favoritesById, Map<Long, UserProfile> usersById) {
        Favorite favorite = favoritesById.get(new FavoriteEmbeddedId(reminder.getUserId(), reminder.getListingId()));
        if (favorite == null) {
            throw new IllegalStateException("Favorite not found for reminder id=" + reminder.getId());
        }

        UserProfile user = usersById.get(reminder.getUserId());
        if (user == null) {
            throw new IllegalStateException("User not found for reminder id=" + reminder.getId());
        }

        String message = buildReminderMessage(reminder, favorite);
        NotificationDto notification = buildNotification(reminder, user, message);
        notificationService.sendNotification(notification);
        reminderPersistenceService.markAsSent(reminder);

        log.debug("Reminder sent id={}, userId={}, listingId={}, interval={}, viewingDate={}",
                reminder.getId(), reminder.getUserId(), reminder.getListingId(),
                reminder.getIntervalType(), favorite.getViewingDate());
    }

    /**
     * Собирает информативный текст напоминания: дата и время просмотра, за какое время до него
     * пришло напоминание и краткая карточка объявления (адрес, параметры, цена, ссылка).
     */
    private String buildReminderMessage(Reminder reminder, Favorite favorite) {
        StringBuilder builder = new StringBuilder();

        Instant viewingDate = favorite.getViewingDate();
        if (viewingDate != null) {
            builder.append("🗓 <b>")
                    .append(VIEWING_DATE_FORMAT.format(viewingDate.atZone(botZone)))
                    .append("</b> — ")
                    .append(leadPhrase(reminder.getIntervalType()));
        } else {
            builder.append(capitalize(leadPhrase(reminder.getIntervalType())));
        }

        String listingCard = buildListingCard(favorite.getListing());
        if (!listingCard.isBlank()) {
            builder.append("\n\n").append(listingCard);
        }

        return builder.toString();
    }

    private String leadPhrase(ReminderIntervalType intervalType) {
        if (intervalType == null) {
            return "напоминание о просмотре";
        }
        return switch (intervalType) {
            case BEFORE_30_MIN -> "напоминание за 30 минут";
            case BEFORE_1_HOUR -> "напоминание за час";
            case BEFORE_1_DAY -> "напоминание за сутки";
        };
    }

    private String buildListingCard(Listing listing) {
        if (listing == null) {
            return "";
        }
        try {
            return ParserMessageUtil.buildMessage(parserResponseMapper.toDto(listing));
        } catch (Exception e) {
            log.warn("Failed to build listing card for reminder, listingId={}: {}", listing.getId(), e.getMessage());
            return buildFallbackListingCard(listing);
        }
    }

    private String buildFallbackListingCard(Listing listing) {
        List<String> lines = new ArrayList<>();
        String headline = joinNonBlank(" // ", escapeHtml(listing.getTitle()), escapeHtml(listing.getStreet()));
        if (!headline.isBlank()) {
            lines.add(headline);
        }
        if (StringUtils.hasText(listing.getSourceUrl())) {
            lines.add("<a href=\"" + escapeHtml(listing.getSourceUrl()) + "\">" + LINK_LABEL + "</a>");
        }
        return String.join("\n", lines);
    }

    private NotificationDto buildNotification(Reminder reminder, UserProfile user, String message) {
        NotificationEngine engine = reminder.getNotificationTemplate() != null
                ? reminder.getNotificationTemplate().getEngine()
                : null;

        String receiver = user.getId().toString();
        if (NotificationEngine.TELEGRAM_BOT.equals(engine)) {
            receiver = cryptoService.decrypt(user.getTelegramId());
        }

        return NotificationDto.builder()
                .userId(reminder.getUserId())
                .receiver(receiver)
                .type(NotificationType.FAVORITE_REMINDER)
                .engine(engine)
                .templateParams(Map.of("message", message))
                .deduplicationKey("REMINDER:" + reminder.getId())
                .build();
    }

    private static String joinNonBlank(String separator, String... values) {
        List<String> nonBlank = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                nonBlank.add(value);
            }
        }
        return String.join(separator, nonBlank);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
