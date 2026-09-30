package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserFilterRepository;
import com.rentalert.identity_ms.service.NewListingNotificationService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewListingNotificationServiceImpl implements NewListingNotificationService {

    private static final String PRODUCER = "identity-ms";

    private final UserFilterRepository userFilterRepository;
    private final OutboxPublisher outboxPublisher;
    private final QuietHoursPolicy quietHoursPolicy;

    private static boolean matchesPrice(UserFilter filter, ListingCreatedEvent listing) {
        if (filter.getPriceFrom() == null && filter.getPriceTo() == null) {
            return true;
        }
        BigDecimal price = filter.getCurrency() == null ? null : switch (filter.getCurrency()) {
            case USD -> listing.priceUsd();
            case BYN -> listing.priceByn();
            // processing-ms converts only between USD and BYN
            case EUR -> null;
        };
        if (price == null) {
            log.debug("No {} price for listing {}, filter {} skipped", filter.getCurrency(), listing.listingId(),
                filter.getId());
            return false;
        }
        return (filter.getPriceFrom() == null || price.compareTo(BigDecimal.valueOf(filter.getPriceFrom())) >= 0)
            && (filter.getPriceTo() == null || price.compareTo(BigDecimal.valueOf(filter.getPriceTo())) <= 0);
    }

    private static boolean isEligible(UserProfile profile) {
        return StringUtils.hasText(profile.getTelegramId())
            && profile.getSettings().getRoleName() != UserRole.BLOCKED
            && profile.getSettings().getBotStatus() == BotStatus.ACTIVE;
    }

    @Override
    @Transactional
    public void notifyMatchingUsers(ListingCreatedEvent listing) {
        if (!StringUtils.hasText(listing.message())) {
            log.warn("Listing {} came without a notification message, nobody is notified", listing.listingId());
            return;
        }
        Float area = listing.category() == ListingCategory.ROOM ? listing.roomArea() : listing.areaTotal();
        List<UserFilter> candidates = userFilterRepository.findActiveFiltersByRoomsAndArea(
            listing.rooms(), area, listing.city(), listing.category());

        // A user with several matching filters still gets the listing once
        Set<Long> notifiedUsers = new HashSet<>();
        Instant now = Instant.now();
        for (UserFilter filter : candidates) {
            UserProfile profile = filter.getUserProfile();
            if (!matchesPrice(filter, listing) || !isEligible(profile)) {
                continue;
            }
            publishListingReceived(profile, filter);
            if (notifiedUsers.add(profile.getId())) {
                outboxPublisher.publish(KafkaTopics.NOTIFICATION_REQUESTS, String.valueOf(profile.getId()),
                    buildRequest(profile, listing, now));
            }
        }
        log.info("Listing {} matched {} filters, {} users notified", listing.listingId(), candidates.size(),
            notifiedUsers.size());
    }

    private void publishListingReceived(UserProfile profile, UserFilter filter) {
        UserActionEvent event = UserActionEvent.builder()
            .event(UserEvent.LISTING_RECEIVED)
            .filterId(filter.getId())
            .build();
        event.setInitiatorId(profile.getId());
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(profile.getId()), event);
    }

    private NotificationRequest buildRequest(UserProfile profile, ListingCreatedEvent listing, Instant now) {
        return NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(profile.getId())
            .type(NotificationType.NEW_LISTING_MATCHED)
            .templateParams(Map.of("message", listing.message()))
            .deduplicationKey("listing:%d:user:%d".formatted(listing.listingId(), profile.getId()))
            // telegramId is stored encrypted with the key notification-ms decrypts it with
            .receiver(new NotificationRequest.Receiver(profile.getTelegramId(), null))
            .listingId(listing.listingId())
            .sendAt(quietHoursPolicy.resumeAt(profile.getSettings(), now).orElse(null))
            .build();
    }
}
