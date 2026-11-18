package com.rentalert.roommate_ms.service.impl;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateFeedCursor;
import com.rentalert.roommate_ms.dto.RoommateFeedPage;
import com.rentalert.roommate_ms.dto.RoommateListingCardView;
import com.rentalert.roommate_ms.mapper.RoommateFeedMapper;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.RoommateFeedService;
import com.rentalert.roommate_ms.service.RoommateProfileAccess;
import com.rentalert.roommate_ms.service.RoommateScoringService;
import com.rentalert.roommate_ms.service.RoommateUserBriefs;
import com.rentalert.roommate_ms.util.RoommateAgeUtil;
import com.rentalert.roommate_ms.util.RoommateGenderPrefUtil;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateFeedServiceImpl implements RoommateFeedService {

    private static final int POOL_CAP = 200;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final RoommateProfileRepository profileRepository;
    private final RoommateListingRepository listingRepository;
    private final RoommateScoringService scoringService;
    private final RoommateFeedMapper feedMapper;
    private final RoommateProfileAccess profileAccess;
    private final RoommateUserBriefs roommateUserBriefs;

    @Override
    @Transactional(readOnly = true)
    public RoommateFeedPage<RoommateCandidateView> candidates(Long viewerId, String cursor, Integer size) {
        RoommateProfile viewer = profileAccess.requireReady(viewerId);
        int paramsHash = candidateParamsHash(viewer);

        List<RoommateProfile> pool = profileRepository.findCandidatePool(
            viewerId,
            viewer.getCity(),
            viewer.getBudgetMin(), viewer.getBudgetMax(),
            viewer.getMoveInFrom(), viewer.getMoveInTo(),
            RoommateGenderPrefUtil.resolve(viewer.getGenderPref()),
            viewer.getGender().name(),
            RoommateScenario.PARTNER,
            PageRequest.of(0, POOL_CAP));

        List<Ranked<RoommateProfile>> ranked = pool.stream()
            .map(candidate -> new Ranked<>(scoringService.score(viewer, candidate), candidate.getCreatedAt(), candidate.getUserId(), candidate))
            .toList();
        Slice<RoommateProfile> slice = paginate(ranked, clampSize(size), parseCursor(cursor, paramsHash), paramsHash);

        Map<Long, UserBriefResponse> authors = findBriefs(slice.items().stream().map(r -> r.entity().getUserId()).toList());
        List<RoommateCandidateView> views = slice.items().stream()
            .map(r -> feedMapper.toCandidateView(r.entity(), authors.get(r.entity().getUserId()), r.score(),
                scoringService.sharedTags(viewer, r.entity())))
            .toList();
        log.info("Roommate candidate feed: viewer={}, pool={}, returned={}, hasMore={}", viewerId, pool.size(), views.size(), slice.hasMore());
        return new RoommateFeedPage<>(views, slice.nextCursor(), slice.hasMore());
    }

    @Override
    @Transactional(readOnly = true)
    public RoommateFeedPage<RoommateListingCardView> listings(Long viewerId, String cursor, Integer size) {
        RoommateProfile viewer = profileAccess.requireReady(viewerId);
        int paramsHash = listingParamsHash(viewer);

        List<RoommateListing> pool = listingRepository.findFeedPool(
            viewerId,
            viewer.getCity(),
            viewer.getBudgetMin(), viewer.getBudgetMax(),
            viewer.getGender().name(),
            RoommateAgeUtil.age(viewer.getBirthYear()),
            PageRequest.of(0, POOL_CAP));

        List<Ranked<RoommateListing>> ranked = pool.stream()
            .map(listing -> new Ranked<>(scoringService.scoreListing(viewer, listing), listing.getCreatedAt(), listing.getId(), listing))
            .toList();
        Slice<RoommateListing> slice = paginate(ranked, clampSize(size), parseCursor(cursor, paramsHash), paramsHash);

        Map<Long, UserBriefResponse> authors = findBriefs(slice.items().stream().map(r -> r.entity().getAuthorUserId()).distinct().toList());
        List<RoommateListingCardView> views = slice.items().stream()
            .map(r -> feedMapper.toListingCardView(r.entity(), authors.get(r.entity().getAuthorUserId()), r.score()))
            .toList();
        log.info("Roommate listing feed: viewer={}, pool={}, returned={}, hasMore={}", viewerId, pool.size(), views.size(), slice.hasMore());
        return new RoommateFeedPage<>(views, slice.nextCursor(), slice.hasMore());
    }

    private record Ranked<E>(int score, Instant createdAt, long id, E entity) {

        Ranked {
            createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        }
    }

    private record Slice<E>(List<Ranked<E>> items, String nextCursor, boolean hasMore) {

    }

    private static <E> Slice<E> paginate(List<Ranked<E>> ranked, int limit, RoommateFeedCursor cursor, int paramsHash) {
        Comparator<Ranked<E>> order = Comparator.<Ranked<E>>comparingInt(Ranked::score).reversed()
            .thenComparing(Ranked::createdAt, Comparator.reverseOrder())
            .thenComparing(Ranked::id, Comparator.reverseOrder());

        List<Ranked<E>> tail = ranked.stream()
            .sorted(order)
            .filter(r -> cursor == null || cursor.isBefore(r.score(), r.createdAt(), r.id()))
            .toList();

        boolean hasMore = tail.size() > limit;
        List<Ranked<E>> items = hasMore ? tail.subList(0, limit) : tail;
        String next = null;
        if (hasMore) {
            Ranked<E> last = items.getLast();
            next = new RoommateFeedCursor(last.score(), last.createdAt(), last.id(), paramsHash).encode();
        }
        return new Slice<>(items, next, hasMore);
    }

    private Map<Long, UserBriefResponse> findBriefs(List<Long> userIds) {
        return roommateUserBriefs.find(userIds);
    }

    private RoommateFeedCursor parseCursor(String token, int currentHash) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RoommateFeedCursor decoded = RoommateFeedCursor.decode(token);
        if (decoded.paramsHash() != currentHash) {
            log.info("Feed cursor params changed, restarting feed");
            return null;
        }
        return decoded;
    }

    private static int candidateParamsHash(RoommateProfile viewer) {
        return Objects.hash(
            viewer.getCity() == null ? null : viewer.getCity().toLowerCase(Locale.ROOT),
            viewer.getBudgetMin(), viewer.getBudgetMax(),
            viewer.getMoveInFrom(), viewer.getMoveInTo(),
            Arrays.hashCode(viewer.getGenderPref()),
            viewer.getGender().name());
    }

    private static int listingParamsHash(RoommateProfile viewer) {
        return Objects.hash(
            viewer.getCity() == null ? null : viewer.getCity().toLowerCase(Locale.ROOT),
            viewer.getBudgetMin(), viewer.getBudgetMax(),
            viewer.getGender().name(), viewer.getBirthYear());
    }

    private static int clampSize(Integer size) {
        return size == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(MAX_PAGE_SIZE, size));
    }
}
