package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.redis.service.CacheService;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateFeedCursor;
import com.rentalert.roommate_ms.dto.RoommateFeedPage;
import com.rentalert.roommate_ms.mapper.RoommateFeedMapper;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.impl.RoommateFeedServiceImpl;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RoommateFeedServiceImplTests {

    private final RoommateProfileRepository profileRepository = mock(RoommateProfileRepository.class);
    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final RoommateScoringService scoringService = mock(RoommateScoringService.class);
    private final RoommateProfileAccess profileAccess = mock(RoommateProfileAccess.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final RoommateUserBriefs roommateUserBriefs =
        new RoommateUserBriefs(identityMsService, new RoommateBatchCache(mock(CacheService.class)));
    private final RoommateFeedService service = new RoommateFeedServiceImpl(profileRepository, listingRepository, scoringService,
        Mappers.getMapper(RoommateFeedMapper.class), profileAccess, roommateUserBriefs);

    private final RoommateProfile viewer = RoommateProfile.builder()
        .userId(1L)
        .gender(Gender.FEMALE)
        .genderPref(new String[]{"MALE", "FEMALE"})
        .city("Минск")
        .budgetMin(300)
        .budgetMax(500)
        .build();

    private static RoommateProfile candidate(long userId, String createdAt) {
        return RoommateProfile.builder().userId(userId).gender(Gender.MALE).createdAt(Instant.parse(createdAt)).build();
    }

    @BeforeEach
    void readyViewer() {
        when(profileAccess.requireReady(1L)).thenReturn(viewer);
        when(scoringService.sharedTags(any(), any())).thenReturn(List.of());
    }

    private void pool(RoommateProfile... candidates) {
        when(profileRepository.findCandidatePool(eq(1L), eq("Минск"), eq(300), eq(500), any(), any(),
            eq(List.of(Gender.MALE, Gender.FEMALE)), eq("FEMALE"), eq(RoommateScenario.PARTNER), any()))
            .thenReturn(List.of(candidates));
    }

    private void score(RoommateProfile candidate, int score) {
        when(scoringService.score(viewer, candidate)).thenReturn(score);
    }

    @Test
    void candidatesAreRankedByScoreThenNewest() {
        RoommateProfile older = candidate(2L, "2026-09-01T00:00:00Z");
        RoommateProfile newer = candidate(3L, "2026-09-02T00:00:00Z");
        RoommateProfile best = candidate(4L, "2026-08-01T00:00:00Z");
        pool(older, newer, best);
        score(older, 60);
        score(newer, 60);
        score(best, 90);
        when(identityMsService.findUserBriefs(List.of(4L, 3L, 2L)))
            .thenReturn(List.of(new UserBriefResponse(3L, "anna", "a.jpg"), new UserBriefResponse(4L, "olga", null)));

        RoommateFeedPage<RoommateCandidateView> page = service.candidates(1L, null, null);

        assertThat(page.items()).extracting(RoommateCandidateView::userId).containsExactly(4L, 3L, 2L);
        assertThat(page.items()).extracting(RoommateCandidateView::username).containsExactly("olga", "anna", null);
        assertThat(page.items().getFirst().score()).isEqualTo(90);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void cursorContinuesWhereThePreviousPageEnded() {
        RoommateProfile a = candidate(2L, "2026-09-01T00:00:00Z");
        RoommateProfile b = candidate(3L, "2026-09-02T00:00:00Z");
        RoommateProfile c = candidate(4L, "2026-09-03T00:00:00Z");
        pool(a, b, c);
        score(a, 50);
        score(b, 70);
        score(c, 90);

        RoommateFeedPage<RoommateCandidateView> first = service.candidates(1L, null, 2);
        RoommateFeedPage<RoommateCandidateView> second = service.candidates(1L, first.nextCursor(), 2);

        assertThat(first.items()).extracting(RoommateCandidateView::userId).containsExactly(4L, 3L);
        assertThat(first.hasMore()).isTrue();
        assertThat(second.items()).extracting(RoommateCandidateView::userId).containsExactly(2L);
        assertThat(second.hasMore()).isFalse();
    }

    @Test
    void cursorHashDoesNotDependOnEnumIdentity() {
        pool(candidate(2L, "2026-09-01T00:00:00Z"), candidate(3L, "2026-09-02T00:00:00Z"));

        String cursor = service.candidates(1L, null, 1).nextCursor();

        int expected = Objects.hash("минск", 300, 500, null, null, Arrays.hashCode(new String[]{"MALE", "FEMALE"}), "FEMALE");
        assertThat(RoommateFeedCursor.decode(cursor).paramsHash()).isEqualTo(expected);
    }

    @Test
    void editedProfileRestartsTheFeed() {
        RoommateProfile a = candidate(2L, "2026-09-01T00:00:00Z");
        RoommateProfile b = candidate(3L, "2026-09-02T00:00:00Z");
        pool(a, b);
        String staleCursor = new RoommateFeedCursor(0, Instant.EPOCH, 0L, 12345).encode();

        RoommateFeedPage<RoommateCandidateView> page = service.candidates(1L, staleCursor, 20);

        assertThat(page.items()).hasSize(2);
    }

    @Test
    void feedSurvivesIdentityOutage() {
        pool(candidate(2L, "2026-09-01T00:00:00Z"));
        when(identityMsService.findUserBriefs(anyList())).thenThrow(new UpstreamHttpException("unavailable", 503, null));

        assertThat(service.candidates(1L, null, null).items())
            .singleElement()
            .satisfies(view -> {
                assertThat(view.userId()).isEqualTo(2L);
                assertThat(view.username()).isNull();
            });
    }

    @Test
    void listingCardsCarryAuthorBrief() {
        RoommateListing listing = RoommateListing.builder().authorUserId(7L).city("Минск").houseRules(new String[]{"quiet"}).build();
        listing.setId(11L);
        when(listingRepository.findFeedPool(eq(1L), eq("Минск"), eq(300), eq(500), eq("FEMALE"), any(), any()))
            .thenReturn(List.of(listing));
        when(scoringService.scoreListing(viewer, listing)).thenReturn(80);
        when(identityMsService.findUserBriefs(List.of(7L))).thenReturn(List.of(new UserBriefResponse(7L, "ivan", "i.jpg")));

        assertThat(service.listings(1L, null, null).items())
            .singleElement()
            .satisfies(card -> {
                assertThat(card.id()).isEqualTo(11L);
                assertThat(card.authorUsername()).isEqualTo("ivan");
                assertThat(card.authorPhotoUrl()).isEqualTo("i.jpg");
                assertThat(card.houseRules()).containsExactly("quiet");
                assertThat(card.score()).isEqualTo(80);
            });
    }
}
