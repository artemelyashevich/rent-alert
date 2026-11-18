package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.messaging.NotificationRequestPublisher;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.repository.UserFilterRepository;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.NewListingNotificationService;
import com.rentalert.internal.client.service.RoommateMsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private final NotificationRequestPublisher notificationRequestPublisher;
    private final QuietHoursPolicy quietHoursPolicy;
    private final RoommateMsService roommateMsService;
    private final UserEventPublisher userEventPublisher;
    private final UserProfileRepository userProfileRepository;

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
        Map<Long, List<UserProfile>> sharedRecipients = loadSharedRecipients(listing, candidates);

        // A user with several matching filters still gets the listing once
        Set<Long> notifiedUsers = new HashSet<>();
        Instant now = Instant.now();
        for (UserFilter filter : candidates) {
            if (!matchesPrice(filter, listing)) {
                continue;
            }
            for (UserProfile profile : recipients(filter, sharedRecipients)) {
                if (!isEligible(profile)) {
                    continue;
                }
                userEventPublisher.publish(UserEvent.LISTING_RECEIVED, profile.getId(), filter.getId());
                if (notifiedUsers.add(profile.getId())) {
                    notificationRequestPublisher.publishListingMatch(buildRequest(profile, listing, now));
                }
            }
        }
        log.info("Listing {} matched {} filters, {} users notified", listing.listingId(), candidates.size(),
            notifiedUsers.size());
    }

    private static List<UserProfile> recipients(UserFilter filter, Map<Long, List<UserProfile>> sharedRecipients) {
        if (filter.getUserProfile() != null) {
            return List.of(filter.getUserProfile());
        }
        return sharedRecipients.getOrDefault(filter.getOwnerMatchId(), List.of());
    }

    private Map<Long, List<UserProfile>> loadSharedRecipients(ListingCreatedEvent listing, List<UserFilter> candidates) {
        List<Long> matchIds = candidates.stream()
            .filter(filter -> filter.getUserProfile() == null && filter.getOwnerMatchId() != null)
            .map(UserFilter::getOwnerMatchId)
            .distinct()
            .toList();
        if (matchIds.isEmpty()) {
            return Map.of();
        }

        List<RoommateMatchParticipantsDto> matches;
        try {
            matches = roommateMsService.findMatchParticipants(matchIds);
        } catch (RuntimeException e) {
            log.warn("roommate-ms unavailable, shared filters skipped for listing {}", listing.listingId(), e);
            return Map.of();
        }

        List<RoommateMatchParticipantsDto> unlocked = matches.stream()
            .filter(match -> match.contactsUnlockedAt() != null)
            .toList();
        Set<Long> participantIds = unlocked.stream()
            .flatMap(match -> Stream.of(match.userLoId(), match.userHiId()))
            .collect(Collectors.toSet());
        Map<Long, UserProfile> profiles = userProfileRepository.findAllById(participantIds).stream()
            .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        Map<Long, List<UserProfile>> recipients = new HashMap<>();
        for (RoommateMatchParticipantsDto match : unlocked) {
            recipients.put(match.matchId(), Stream.of(match.userLoId(), match.userHiId())
                .map(profiles::get)
                .filter(Objects::nonNull)
                .toList());
        }
        return recipients;
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
