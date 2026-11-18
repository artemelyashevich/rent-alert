package com.rentalert.roommate_ms.service;

import static com.rentalert.roommate_ms.domain.RoommateDecision.INTERESTED;
import static com.rentalert.roommate_ms.domain.RoommateDecision.PENDING;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.CHATTING;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MATCHED;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.MOVED_IN;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.VIEWING_TOGETHER;
import static com.rentalert.roommate_ms.domain.RoommateMatchStatus.WAITING_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.kafka.audit.UserAudit;
import com.rentalert.redis.service.CacheService;
import com.rentalert.roommate_ms.domain.RoommateDecision;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateMatchParticipant;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import com.rentalert.roommate_ms.dto.RoommateContactResponse;
import com.rentalert.roommate_ms.dto.RoommateMatchAction;
import com.rentalert.roommate_ms.dto.RoommateMatchView;
import com.rentalert.roommate_ms.dto.RoommateTransitionCommand;
import com.rentalert.roommate_ms.event.RoommateMatchTransitionEvent;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateMatchEventRepository;
import com.rentalert.roommate_ms.repository.RoommateMatchParticipantRepository;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import com.rentalert.roommate_ms.repository.RoommateViewingRepository;
import com.rentalert.roommate_ms.service.impl.RoommateMatchServiceImpl;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

class RoommateMatchServiceImplTests {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final RoommateMatchEventRepository matchEventRepository = mock(RoommateMatchEventRepository.class);
    private final RoommateMatchParticipantRepository participantRepository = mock(RoommateMatchParticipantRepository.class);
    private final RoommateMatchRepository matchRepository = mock(RoommateMatchRepository.class);
    private final RoommateProfileAccess profileAccess = mock(RoommateProfileAccess.class);
    private final RoommateUserBriefs roommateUserBriefs =
        new RoommateUserBriefs(identityMsService, new RoommateBatchCache(mock(CacheService.class)));
    private final RoommateViewingRepository viewingRepository = mock(RoommateViewingRepository.class);

    private RoommateMatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoommateMatchServiceImpl(eventPublisher, identityMsService, listingMsService, listingRepository,
            matchEventRepository, participantRepository, matchRepository, new RoommateMatchStateMachine(), profileAccess,
            roommateUserBriefs, viewingRepository);
    }

    private static RoommateMatch match(long id, long loId, long hiId, RoommateMatchStatus status,
        RoommateDecision loDecision, RoommateDecision hiDecision) {
        return RoommateMatch.builder()
            .id(id)
            .scenario(RoommateScenario.PARTNER)
            .userLoId(loId)
            .userHiId(hiId)
            .contextRef("")
            .loDecision(loDecision)
            .hiDecision(hiDecision)
            .status(status)
            .statusVersion(0L)
            .initiatorUserId(loId)
            .build();
    }

    private static UserBriefResponse brief(long userId) {
        return new UserBriefResponse(userId, "u" + userId, "p" + userId);
    }

    @Test
    void likeOfYourselfIsRejected() {
        assertCode(() -> service.like(1L, 1L), InternalExceptionCode.EX400_ROOMMATE_SELF_ACTION);
    }

    @Test
    void likeCreatesWaitingMatchWhenNoneExists() {
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 2L, RoommateScenario.PARTNER, ""))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING)));
        when(matchRepository.insertIfAbsent(eq("PARTNER"), eq(1L), eq(2L), eq(""), isNull(), eq("INTERESTED"),
            eq("PENDING"), eq("WAITING_RESPONSE"), eq(1L), any(), any())).thenReturn(1);
        when(identityMsService.findUserBriefs(List.of(2L))).thenReturn(List.of(brief(2L)));

        RoommateMatchView view = service.like(1L, 2L);

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.status()).isEqualTo(WAITING_RESPONSE);
        assertThat(view.myDecision()).isEqualTo(INTERESTED);
        assertThat(view.counterpartyDecision()).isEqualTo(PENDING);
        assertThat(view.counterpartyUserId()).isEqualTo(2L);
        assertThat(view.counterpartyUsername()).isEqualTo("u2");
        assertThat(view.counterpartyPhotoUrl()).isEqualTo("p2");
        assertThat(view.initiatedByMe()).isTrue();
        assertThat(view.contactsUnlocked()).isFalse();

        verify(participantRepository, times(2)).save(any(RoommateMatchParticipant.class));
        verify(matchEventRepository).save(any());
        verify(eventPublisher).publishEvent(any(RoommateMatchTransitionEvent.class));
    }

    @Test
    void likeOfMissingOrUnreadyUserIsRejectedBeforeAnyWrite() {
        when(profileAccess.requireExists(2L)).thenThrow(new AppException(
            InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND));

        assertCode(() -> service.like(1L, 2L), InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND);
        verify(matchRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void reciprocalLikePromotesToMatchedAndUnlocksContacts() {
        RoommateMatch existing = match(100L, 1L, 2L, WAITING_RESPONSE, PENDING, INTERESTED);
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 2L, RoommateScenario.PARTNER, ""))
            .thenReturn(Optional.of(existing));
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(existing));

        RoommateMatchView view = service.like(1L, 2L);

        assertThat(existing.getStatus()).isEqualTo(MATCHED);
        assertThat(existing.getContactsUnlockedAt()).isNotNull();
        assertThat(existing.getExpiresAt()).isNull();
        assertThat(view.status()).isEqualTo(MATCHED);
        assertThat(view.contactsUnlocked()).isTrue();
        verify(matchRepository).saveAndFlush(existing);
    }

    @Test
    void repeatedLikeIsIdempotent() {
        RoommateMatch existing = match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING);
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 2L, RoommateScenario.PARTNER, ""))
            .thenReturn(Optional.of(existing));
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(existing));

        RoommateMatchView view = service.like(1L, 2L);

        assertThat(view.status()).isEqualTo(WAITING_RESPONSE);
        verify(matchRepository, never()).saveAndFlush(any());
    }

    @Test
    void likeOnTerminalMatchDoesNotRevive() {
        RoommateMatch closed = match(100L, 1L, 2L, RoommateMatchStatus.DECLINED, RoommateDecision.DECLINED, PENDING);
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 2L, RoommateScenario.PARTNER, ""))
            .thenReturn(Optional.of(closed));
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(closed));

        assertCode(() -> service.like(1L, 2L), InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED);
        verify(matchRepository, never()).saveAndFlush(any());
    }

    @Test
    void raceOnCreateAppliesLikeToTheConcurrentlyCreatedMatchWithoutCreatingAnotherPair() {
        RoommateMatch createdConcurrently = match(100L, 1L, 2L, WAITING_RESPONSE, PENDING, INTERESTED);
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 2L, RoommateScenario.PARTNER, ""))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(createdConcurrently));
        when(matchRepository.insertIfAbsent(eq("PARTNER"), eq(1L), eq(2L), eq(""), isNull(), eq("INTERESTED"),
            eq("PENDING"), eq("WAITING_RESPONSE"), eq(1L), any(), any())).thenReturn(0);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(createdConcurrently));

        RoommateMatchView view = service.like(1L, 2L);

        assertThat(view.status()).isEqualTo(MATCHED);
        assertThat(view.myDecision()).isEqualTo(INTERESTED);
        assertThat(createdConcurrently.getContactsUnlockedAt()).isNotNull();
        verify(matchRepository).saveAndFlush(createdConcurrently);
        verify(participantRepository, never()).save(any());
    }

    @Test
    void likeListingCreatesRoomMatchWithTheAuthorAsCounterparty() {
        RoommateListing listing = RoommateListing.builder().id(7L).authorUserId(9L).status(RoommateListingStatus.ACTIVE).build();
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        RoommateMatch roomMatch = RoommateMatch.builder().id(101L).scenario(RoommateScenario.ROOM).userLoId(1L).userHiId(9L)
            .contextRef("7").roommateListing(listing).loDecision(INTERESTED).hiDecision(PENDING)
            .status(WAITING_RESPONSE).statusVersion(0L).initiatorUserId(1L).build();
        when(matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(1L, 9L, RoommateScenario.ROOM, "7"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(roomMatch));
        when(matchRepository.insertIfAbsent(eq("ROOM"), eq(1L), eq(9L), eq("7"), eq(7L), eq("INTERESTED"),
            eq("PENDING"), eq("WAITING_RESPONSE"), eq(1L), any(), any())).thenReturn(1);

        RoommateMatchView view = service.likeListing(1L, 7L);

        assertThat(view.scenario()).isEqualTo(RoommateScenario.ROOM);
        assertThat(view.counterpartyUserId()).isEqualTo(9L);
        assertThat(view.roommateListingId()).isEqualTo(7L);
    }

    @Test
    void likeListingRejectsMissingListing() {
        when(listingRepository.findById(7L)).thenReturn(Optional.empty());

        assertCode(() -> service.likeListing(1L, 7L), InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND);
    }

    @Test
    void likeListingRejectsInactiveListing() {
        RoommateListing listing = RoommateListing.builder().id(7L).authorUserId(9L).status(RoommateListingStatus.PAUSED).build();
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));

        assertCode(() -> service.likeListing(1L, 7L), InternalExceptionCode.EX400_ROOMMATE_LISTING_NOT_ACTIVE);
    }

    @Test
    void transitionByNonParticipantIsForbidden() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.transition(100L, 999L, RoommateMatchAction.ACCEPT, null, null),
            InternalExceptionCode.EX403_ROOMMATE_NOT_PARTICIPANT);
    }

    @Test
    void transitionWithStaleVersionIsConflict() {
        RoommateMatch m = match(100L, 1L, 2L, WAITING_RESPONSE, PENDING, INTERESTED);
        m.setStatusVersion(5L);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.ACCEPT, null, 4L),
            InternalExceptionCode.EX409_ROOMMATE_MATCH_VERSION_MISMATCH);
        verify(matchRepository, never()).saveAndFlush(any());
    }

    @Test
    void transitionIllegalForCurrentStatusIsConflict() {
        RoommateMatch m = match(100L, 1L, 2L, RoommateMatchStatus.INTERESTED, INTERESTED, PENDING);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.START_CHAT, null, null),
            InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED);
    }

    @Test
    void acceptPromotesWaitingMatchWhenCounterpartyAlreadyInterested() {
        RoommateMatch m = match(100L, 1L, 2L, WAITING_RESPONSE, PENDING, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.ACCEPT, null, null);

        assertThat(view.status()).isEqualTo(MATCHED);
        assertThat(view.myDecision()).isEqualTo(INTERESTED);
        assertThat(m.getContactsUnlockedAt()).isNotNull();
    }

    @Test
    void declineClosesMatchAndReleasesCommitment() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        RoommateMatchParticipant p1 = RoommateMatchParticipant.builder().userId(1L).committed(true).build();
        RoommateMatchParticipant p2 = RoommateMatchParticipant.builder().userId(2L).committed(true).build();
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(participantRepository.findByMatchId(100L)).thenReturn(List.of(p1, p2));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.DECLINE, null, null);

        assertThat(view.status()).isEqualTo(RoommateMatchStatus.DECLINED);
        assertThat(view.myDecision()).isEqualTo(RoommateDecision.DECLINED);
        assertThat(p1.isCommitted()).isFalse();
        assertThat(p2.isCommitted()).isFalse();
    }

    @Test
    void startChatMovesToChattingAndCommitsBothParticipants() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        RoommateMatchParticipant p1 = RoommateMatchParticipant.builder().userId(1L).committed(false).build();
        RoommateMatchParticipant p2 = RoommateMatchParticipant.builder().userId(2L).committed(false).build();
        when(participantRepository.findByMatchId(100L)).thenReturn(List.of(p1, p2));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.START_CHAT, null, 0L);

        assertThat(view.status()).isEqualTo(CHATTING);
        assertThat(p1.isCommitted()).isTrue();
        assertThat(p2.isCommitted()).isTrue();
        verify(participantRepository).saveAllAndFlush(List.of(p1, p2));
    }

    @Test
    void startChatConflictingWithExistingCommitmentIsConflict() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(participantRepository.findByMatchId(100L)).thenReturn(List.of(
            RoommateMatchParticipant.builder().userId(1L).build(),
            RoommateMatchParticipant.builder().userId(2L).build()));
        when(participantRepository.saveAllAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq violation"));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.START_CHAT, null, null),
            InternalExceptionCode.EX409_ROOMMATE_ALREADY_COMMITTED);
    }

    @Test
    void scheduleViewingWithoutDateIsBadRequest() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.SCHEDULE_VIEWING, RoommateTransitionCommand.empty(), null),
            InternalExceptionCode.EX400_VIEWING_DATE_REQUIRED);
    }

    @Test
    void scheduleViewingInThePastIsBadRequest() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        RoommateTransitionCommand cmd = new RoommateTransitionCommand(null, "ул. Тестовая", Instant.now().minusSeconds(60));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.SCHEDULE_VIEWING, cmd, null),
            InternalExceptionCode.EX400_VIEWING_DATE_IN_PAST);
    }

    @Test
    void scheduleViewingWithoutListingMovesToViewingTogether() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(viewingRepository.save(any())).thenAnswer(inv -> {
            RoommateViewing v = inv.getArgument(0);
            v.setId(555L);
            return v;
        });
        RoommateTransitionCommand cmd = new RoommateTransitionCommand(null, "ул. Тестовая, 1", Instant.now().plusSeconds(3600));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.SCHEDULE_VIEWING, cmd, null);

        assertThat(view.status()).isEqualTo(VIEWING_TOGETHER);
        verify(listingMsService, never()).findCards(anyCollection());
        verify(eventPublisher).publishEvent(argThat((RoommateMatchTransitionEvent e) -> Long.valueOf(555L).equals(e.roommateViewingId())));
    }

    @Test
    void scheduleViewingCopiesListingSnapshotIntoViewing() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        ListingDto card = ListingDto.builder()
            .id(77L)
            .title("Светлая комната")
            .street("ул. Ленина, 1")
            .metroStation("Ленинская")
            .price(new BigDecimal("450.00"))
            .currency(Currency.BYN)
            .previewPhoto("cover.jpg")
            .build();
        when(listingMsService.findCards(List.of(77L))).thenReturn(List.of(card));
        when(viewingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RoommateTransitionCommand cmd = new RoommateTransitionCommand(77L, null, Instant.now().plusSeconds(3600));

        service.transition(100L, 1L, RoommateMatchAction.SCHEDULE_VIEWING, cmd, null);

        verify(viewingRepository).save(argThat((RoommateViewing v) ->
            Long.valueOf(77L).equals(v.getListingId())
                && "Светлая комната".equals(v.getListingTitle())
                && "ул. Ленина, 1".equals(v.getListingStreet())
                && "Ленинская".equals(v.getListingMetroStation())
                && new BigDecimal("450.00").compareTo(v.getListingPrice()) == 0
                && v.getListingCurrency() == Currency.BYN
                && "cover.jpg".equals(v.getListingCoverPhotoUrl())));
    }

    @Test
    void scheduleViewingForMissingListingIsNotFound() {
        RoommateMatch m = match(100L, 1L, 2L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(listingMsService.findCards(List.of(77L))).thenReturn(List.of());
        RoommateTransitionCommand cmd = new RoommateTransitionCommand(77L, null, Instant.now().plusSeconds(3600));

        assertCode(() -> service.transition(100L, 1L, RoommateMatchAction.SCHEDULE_VIEWING, cmd, null),
            InternalExceptionCode.EX404_LISTING_NOT_FOUND);
        verify(viewingRepository, never()).save(any());
    }

    @Test
    void cancelViewingCancelsScheduledViewingAndReturnsToChatting() {
        RoommateMatch m = match(100L, 1L, 2L, VIEWING_TOGETHER, INTERESTED, INTERESTED);
        RoommateViewing scheduled = RoommateViewing.builder().match(m).viewingDate(Instant.now().plusSeconds(60)).createdByUserId(1L).build();
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(viewingRepository.findFirstByMatchIdAndStatusOrderByViewingDateAsc(100L,
            RoommateViewingStatus.SCHEDULED)).thenReturn(Optional.of(scheduled));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.CANCEL_VIEWING, null, null);

        assertThat(view.status()).isEqualTo(CHATTING);
        assertThat(scheduled.getStatus()).isEqualTo(RoommateViewingStatus.CANCELLED);
        verify(viewingRepository).save(scheduled);
    }

    @Test
    void confirmMoveInFromOneSideStaysInViewingTogether() {
        RoommateMatch m = match(100L, 1L, 2L, VIEWING_TOGETHER, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));

        RoommateMatchView view = service.transition(100L, 1L, RoommateMatchAction.CONFIRM_MOVE_IN, null, null);

        assertThat(view.status()).isEqualTo(VIEWING_TOGETHER);
        assertThat(view.iConfirmedMoveIn()).isTrue();
        assertThat(view.counterpartyConfirmedMoveIn()).isFalse();
        assertThat(m.getStatus()).isEqualTo(VIEWING_TOGETHER);
    }

    @Test
    void confirmMoveInFromBothSidesMovesInAndArchivesOtherMatches() {
        RoommateMatch m = match(100L, 1L, 2L, VIEWING_TOGETHER, INTERESTED, INTERESTED);
        m.setLoMoved(true);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(matchRepository.findOtherActive(anyLong(), any(), eq(100L), any())).thenReturn(List.of());

        RoommateMatchView view = service.transition(100L, 2L, RoommateMatchAction.CONFIRM_MOVE_IN, null, null);

        assertThat(view.status()).isEqualTo(MOVED_IN);
        assertThat(m.getStatus()).isEqualTo(MOVED_IN);
        verify(matchRepository, times(2)).findOtherActive(anyLong(), eq(RoommateScenario.PARTNER), eq(100L), any());
    }

    @Test
    void confirmMoveInArchivesOtherActiveMatchesOfBothParticipants() {
        RoommateMatch m = match(100L, 1L, 2L, VIEWING_TOGETHER, INTERESTED, INTERESTED);
        m.setLoMoved(true);
        RoommateMatch other = match(200L, 1L, 3L, CHATTING, INTERESTED, INTERESTED);
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(m));
        when(matchRepository.findOtherActive(eq(1L), any(), eq(100L), any())).thenReturn(List.of(other));
        when(matchRepository.findOtherActive(eq(2L), any(), eq(100L), any())).thenReturn(List.of());

        service.transition(100L, 2L, RoommateMatchAction.CONFIRM_MOVE_IN, null, null);

        assertThat(other.getStatus()).isEqualTo(RoommateMatchStatus.ARCHIVED);
        verify(matchRepository).save(other);
    }

    @Test
    void getMatchByNonParticipantIsForbidden() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        when(matchRepository.findById(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.getMatch(100L, 999L), InternalExceptionCode.EX403_ROOMMATE_NOT_PARTICIPANT);
    }

    @Test
    void contactsAreLockedUntilMatched() {
        RoommateMatch m = match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING);
        when(matchRepository.findById(100L)).thenReturn(Optional.of(m));

        assertCode(() -> service.getContacts(100L, 1L), InternalExceptionCode.EX403_ROOMMATE_MATCH_NOT_UNLOCKED);
    }

    @Test
    void contactsOfMatchedCounterpartyAreReturned() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        m.setContactsUnlockedAt(Instant.now());
        when(matchRepository.findById(100L)).thenReturn(Optional.of(m));
        when(identityMsService.findUserBriefs(List.of(2L))).thenReturn(List.of(brief(2L)));

        RoommateContactResponse contacts = service.getContacts(100L, 1L);

        assertThat(contacts).isEqualTo(new RoommateContactResponse(2L, "u2", "https://t.me/u2"));
    }

    @Test
    void getContactsIsTransactionalAndAuditedAsContactReveal() throws NoSuchMethodException {
        Method method = RoommateMatchServiceImpl.class.getMethod("getContacts", Long.class, Long.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class).readOnly()).isFalse();
        UserAudit audit = method.getAnnotation(UserAudit.class);
        assertThat(audit).isNotNull();
        assertThat(audit.value()).isEqualTo(UserEvent.ROOMMATE_CONTACT_REVEALED);
        assertThat(audit.userId()).isEqualTo("#requesterUserId");
        assertThat(audit.params()).containsExactly("#matchId", "#requesterUserId");
    }

    @Test
    void contactsWithoutTelegramUsernameHaveNoDeepLink() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        m.setContactsUnlockedAt(Instant.now());
        when(matchRepository.findById(100L)).thenReturn(Optional.of(m));
        when(identityMsService.findUserBriefs(List.of(1L))).thenReturn(List.of(new UserBriefResponse(1L, null, null)));

        RoommateContactResponse contacts = service.getContacts(100L, 2L);

        assertThat(contacts.userId()).isEqualTo(1L);
        assertThat(contacts.username()).isNull();
        assertThat(contacts.telegramDeepLink()).isNull();
    }

    @Test
    void myMatchesCarriesCounterpartyNamesFromOneBatchCall() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        RoommateMatchParticipant participant = RoommateMatchParticipant.builder().match(m).userId(1L).build();
        when(participantRepository.findMine(eq(1L), any())).thenReturn(
            new PageImpl<>(List.of(participant)));
        when(identityMsService.findUserBriefs(List.of(2L))).thenReturn(List.of(brief(2L)));

        var page = service.myMatches(1L, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).singleElement().satisfies(view -> {
            assertThat(view.counterpartyUsername()).isEqualTo("u2");
            assertThat(view.counterpartyPhotoUrl()).isEqualTo("p2");
        });
        verify(identityMsService, times(1)).findUserBriefs(anyCollection());
    }

    @Test
    void myMatchesStillListsMatchesWhenIdentityIsUnavailable() {
        RoommateMatch m = match(100L, 1L, 2L, MATCHED, INTERESTED, INTERESTED);
        RoommateMatchParticipant participant = RoommateMatchParticipant.builder().match(m).userId(1L).build();
        when(participantRepository.findMineByStatuses(eq(1L), any(), any())).thenReturn(
            new PageImpl<>(List.of(participant)));
        when(identityMsService.findUserBriefs(anyCollection())).thenThrow(new IllegalStateException("identity-ms is down"));

        var page = service.myMatches(1L, List.of(MATCHED), PageRequest.of(0, 20));

        assertThat(page.getContent()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo(100L);
            assertThat(view.counterpartyUserId()).isEqualTo(2L);
            assertThat(view.counterpartyUsername()).isNull();
        });
    }

    @Test
    void expireStaleMatchesMovesPastDueWaitingMatchesToExpired() {
        RoommateMatch stale = match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING);
        stale.setExpiresAt(Instant.now().minusSeconds(10));
        when(matchRepository.findByStatusAndExpiresAtBefore(eq(WAITING_RESPONSE), any())).thenReturn(List.of(stale));
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(stale));

        int expired = service.expireStaleMatches();

        assertThat(expired).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(RoommateMatchStatus.EXPIRED);
        assertThat(stale.getExpiresAt()).isNull();
    }

    @Test
    void expireStaleMatchesSkipsMatchesRenewedMeanwhile() {
        RoommateMatch stale = match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING);
        stale.setExpiresAt(Instant.now().minusSeconds(10));
        RoommateMatch renewed = match(100L, 1L, 2L, WAITING_RESPONSE, INTERESTED, PENDING);
        renewed.setExpiresAt(Instant.now().plusSeconds(3600));
        when(matchRepository.findByStatusAndExpiresAtBefore(eq(WAITING_RESPONSE), any())).thenReturn(List.of(stale));
        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(renewed));

        assertThat(service.expireStaleMatches()).isZero();
        assertThat(renewed.getStatus()).isEqualTo(WAITING_RESPONSE);
        verify(matchRepository, never()).save(any());
    }

    private static void assertCode(Runnable action, InternalExceptionCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
