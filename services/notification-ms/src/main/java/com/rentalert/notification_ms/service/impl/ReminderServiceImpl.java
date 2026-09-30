package com.rentalert.notification_ms.service.impl;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.notification.ReminderIntervalType;
import com.rentalert.notification_ms.domain.Reminder;
import com.rentalert.notification_ms.service.NotificationService;
import com.rentalert.notification_ms.service.ReminderPersistenceService;
import com.rentalert.notification_ms.service.ReminderService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final ReminderPersistenceService reminderPersistenceService;

    @Value("${app.bot.timezone:Europe/Minsk}")
    private ZoneId botZone;

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

    @Override
    public void send() {
        List<Reminder> reminders = reminderPersistenceService.findBatchToRemind(BATCH_SIZE);

        if (reminders.isEmpty()) {
            return;
        }

        log.info("Processing {} reminders", reminders.size());

        int sent = 0;
        int failed = 0;
        for (Reminder reminder : reminders) {
            try {
                send(reminder);
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

    private void send(Reminder reminder) {
        NotificationEngine engine = reminder.getNotificationTemplate() != null
            ? reminder.getNotificationTemplate().getEngine()
            : null;
        ReminderTarget target = loadTarget(reminder, engine);

        String message = buildReminderMessage(reminder, target);
        notificationService.sendNotification(NotificationDto.builder()
            .userId(reminder.getUserId())
            .receiver(target.receiver())
            .type(NotificationType.FAVORITE_REMINDER)
            .engine(engine)
            .templateParams(Map.of("message", message))
            .deduplicationKey("REMINDER:" + reminder.getId())
            .build());
        reminderPersistenceService.markAsSent(reminder);

        log.debug("Reminder sent id={}, userId={}, listingId={}, interval={}, viewingDate={}",
            reminder.getId(), reminder.getUserId(), reminder.getListingId(),
            reminder.getIntervalType(), target.viewingDate());
    }

    // TODO: дата просмотра и объявление — из listing-ms (избранное пользователя),
    //  адрес получателя для engine (Telegram id / email) — из identity-ms
    private ReminderTarget loadTarget(Reminder reminder, NotificationEngine engine) {
        throw new IllegalStateException("Reminder data is unavailable: listing-ms and identity-ms are not integrated");
    }

    private String buildReminderMessage(Reminder reminder, ReminderTarget target) {
        StringBuilder builder = new StringBuilder();

        Instant viewingDate = target.viewingDate();
        if (viewingDate != null) {
            builder.append("🗓 <b>")
                .append(VIEWING_DATE_FORMAT.format(viewingDate.atZone(botZone)))
                .append("</b> — ")
                .append(leadPhrase(reminder.getIntervalType()));
        } else {
            builder.append(capitalize(leadPhrase(reminder.getIntervalType())));
        }

        String listingCard = buildListingCard(target.listing());
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

    private String buildListingCard(ListingDto listing) {
        if (listing == null) {
            return "";
        }
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

    private record ReminderTarget(Instant viewingDate, ListingDto listing, String receiver) {

    }
}
