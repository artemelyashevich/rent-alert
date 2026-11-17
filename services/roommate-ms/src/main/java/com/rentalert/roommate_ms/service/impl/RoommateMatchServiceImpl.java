package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.audit.UserAudit;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.roommate_ms.domain.RoommateDecision;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateMatchEvent;
import com.rentalert.roommate_ms.domain.RoommateMatchParticipant;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateParticipantRole;
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
import com.rentalert.roommate_ms.service.RoommateMatchService;
import com.rentalert.roommate_ms.service.RoommateMatchStateMachine;
import com.rentalert.roommate_ms.service.RoommateProfileAccess;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateMatchServiceImpl implements RoommateMatchService {

    private static final Duration INTEREST_TTL = Duration.ofDays(14);

    private static final List<RoommateMatchStatus> ACTIVE_STATUSES = List.of(
        RoommateMatchStatus.INTERESTED, RoommateMatchStatus.WAITING_RESPONSE, RoommateMatchStatus.MATCHED,
        RoommateMatchStatus.CHATTING, RoommateMatchStatus.VIEWING_TOGETHER);

    private final ApplicationEventPublisher eventPublisher;
    private final IdentityMsService identityMsService;
    private final ListingMsService listingMsService;
    private final RoommateListingRepository listingRepository;
    private final RoommateMatchEventRepository matchEventRepository;
    private final RoommateMatchParticipantRepository participantRepository;
    private final RoommateMatchRepository matchRepository;
    private final RoommateMatchStateMachine stateMachine;
    private final RoommateProfileAccess profileAccess;
    private final RoommateViewingRepository viewingRepository;

    @Override
    @Transactional
    public RoommateMatchView like(Long actorUserId, Long targetUserId) {
        assertNotSelf(actorUserId, targetUserId);
        profileAccess.requireReady(actorUserId);
        profileAccess.requireExists(targetUserId);
        return likeInScenario(actorUserId, targetUserId, RoommateScenario.PARTNER, "", null);
    }

    @Override
    @Transactional
    public RoommateMatchView likeListing(Long actorUserId, Long listingId) {
        RoommateListing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (listing.getStatus() != RoommateListingStatus.ACTIVE) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_LISTING_NOT_ACTIVE, HttpStatus.BAD_REQUEST);
        }
        Long authorId = listing.getAuthorUserId();
        assertNotSelf(actorUserId, authorId);
        profileAccess.requireReady(actorUserId);
        return likeInScenario(actorUserId, authorId, RoommateScenario.ROOM, String.valueOf(listingId), listing);
    }

    private RoommateMatchView likeInScenario(
        Long actorUserId, Long otherUserId, RoommateScenario scenario, String contextRef, RoommateListing listing) {

        long lo = Math.min(actorUserId, otherUserId);
        long hi = Math.max(actorUserId, otherUserId);

        Optional<RoommateMatch> existing =
            matchRepository.findByUserLoIdAndUserHiIdAndScenarioAndContextRef(lo, hi, scenario, contextRef);
        if (existing.isPresent()) {
            return applyLikeOnExisting(existing.get().getId(), actorUserId);
        }

        boolean actorIsLo = actorUserId == lo;
        RoommateMatch match = RoommateMatch.builder()
            .scenario(scenario)
            .userLoId(lo)
            .userHiId(hi)
            .contextRef(contextRef)
            .roommateListing(listing)
            .loDecision(actorIsLo ? RoommateDecision.INTERESTED : RoommateDecision.PENDING)
            .hiDecision(actorIsLo ? RoommateDecision.PENDING : RoommateDecision.INTERESTED)
            .status(RoommateMatchStatus.WAITING_RESPONSE)
            .initiatorUserId(actorUserId)
            .expiresAt(Instant.now().plus(INTEREST_TTL))
            .build();

        try {
            match = matchRepository.save(match);
        } catch (DataIntegrityViolationException race) {
            log.info("Race on like creation, refetching. lo={}, hi={}, scenario={}", lo, hi, scenario);
            RoommateMatch created = matchRepository
                .findByUserLoIdAndUserHiIdAndScenarioAndContextRef(lo, hi, scenario, contextRef)
                .orElseThrow(() -> race);
            return applyLikeOnExisting(created.getId(), actorUserId);
        }

        saveParticipant(match, lo, scenario, actorIsLo ? RoommateParticipantRole.INITIATOR : RoommateParticipantRole.RESPONDER);
        saveParticipant(match, hi, scenario, actorIsLo ? RoommateParticipantRole.RESPONDER : RoommateParticipantRole.INITIATOR);

        appendEvent(match, null, match.getStatus(), actorUserId, "like");
        publish(match, null, match.getStatus(), actorUserId, otherUserId, null);

        return view(match, actorUserId);
    }

    private RoommateMatchView applyLikeOnExisting(Long matchId, Long actorUserId) {
        RoommateMatch match = matchRepository.findByIdForUpdate(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MATCH_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (stateMachine.isTerminal(match.getStatus())) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED,
                "match is in terminal status " + match.getStatus(), HttpStatus.CONFLICT);
        }

        boolean actorIsLo = isLo(match, actorUserId);
        RoommateDecision current = actorIsLo ? match.getLoDecision() : match.getHiDecision();
        if (current == RoommateDecision.INTERESTED) {
            return view(match, actorUserId);
        }

        setDecision(match, actorIsLo, RoommateDecision.INTERESTED);

        if (match.getLoDecision() == RoommateDecision.INTERESTED && match.getHiDecision() == RoommateDecision.INTERESTED) {
            promoteToMatched(match, actorUserId);
        } else {
            match.setExpiresAt(Instant.now().plus(INTEREST_TTL));
            publish(match, match.getStatus(), match.getStatus(), actorUserId, counterpartyId(match, actorUserId), null);
        }
        matchRepository.saveAndFlush(match);
        return view(match, actorUserId);
    }

    private void promoteToMatched(RoommateMatch match, Long actorUserId) {
        RoommateMatchStatus from = match.getStatus();
        match.setStatus(RoommateMatchStatus.MATCHED);
        match.setContactsUnlockedAt(Instant.now());
        match.setExpiresAt(null);
        appendEvent(match, from, RoommateMatchStatus.MATCHED, actorUserId, "mutual interest");
        publish(match, from, RoommateMatchStatus.MATCHED, actorUserId, counterpartyId(match, actorUserId), null);
    }

    @Override
    @Transactional
    public RoommateMatchView transition(Long matchId, Long actorUserId, RoommateMatchAction action,
        RoommateTransitionCommand command, Long expectedVersion) {

        RoommateMatch match = matchRepository.findByIdForUpdate(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MATCH_NOT_FOUND, HttpStatus.NOT_FOUND));
        assertParticipant(match, actorUserId);
        if (expectedVersion != null && !expectedVersion.equals(match.getStatusVersion())) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_MATCH_VERSION_MISMATCH, HttpStatus.CONFLICT);
        }

        RoommateTransitionCommand cmd = command == null ? RoommateTransitionCommand.empty() : command;

        switch (action) {
            case ACCEPT -> doAccept(match, actorUserId);
            case DECLINE -> doDecline(match, actorUserId);
            case START_CHAT -> doStartChat(match, actorUserId);
            case SCHEDULE_VIEWING -> doScheduleViewing(match, actorUserId, cmd);
            case CANCEL_VIEWING -> doCancelViewing(match, actorUserId);
            case CONFIRM_MOVE_IN -> doConfirmMoveIn(match, actorUserId);
        }

        matchRepository.saveAndFlush(match);
        return view(match, actorUserId);
    }

    private void doAccept(RoommateMatch match, Long actorUserId) {
        stateMachine.target(RoommateMatchAction.ACCEPT, match.getStatus());
        boolean actorIsLo = isLo(match, actorUserId);
        RoommateDecision current = actorIsLo ? match.getLoDecision() : match.getHiDecision();
        if (current == RoommateDecision.DECLINED) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_TRANSITION_NOT_ALLOWED,
                "already declined", HttpStatus.CONFLICT);
        }
        setDecision(match, actorIsLo, RoommateDecision.INTERESTED);
        if (match.getLoDecision() == RoommateDecision.INTERESTED && match.getHiDecision() == RoommateDecision.INTERESTED) {
            promoteToMatched(match, actorUserId);
        }
    }

    private void doDecline(RoommateMatch match, Long actorUserId) {
        stateMachine.target(RoommateMatchAction.DECLINE, match.getStatus());
        RoommateMatchStatus from = match.getStatus();
        setDecision(match, isLo(match, actorUserId), RoommateDecision.DECLINED);
        match.setStatus(RoommateMatchStatus.DECLINED);
        match.setExpiresAt(null);
        releaseCommitment(match);
        appendEvent(match, from, RoommateMatchStatus.DECLINED, actorUserId, "decline");
        publish(match, from, RoommateMatchStatus.DECLINED, actorUserId, counterpartyId(match, actorUserId), null);
    }

    private void doStartChat(RoommateMatch match, Long actorUserId) {
        stateMachine.target(RoommateMatchAction.START_CHAT, match.getStatus());
        List<RoommateMatchParticipant> participants = participantRepository.findByMatchId(match.getId());
        participants.forEach(p -> p.setCommitted(true));
        try {
            participantRepository.saveAllAndFlush(participants);
        } catch (DataIntegrityViolationException alreadyCommittedElsewhere) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_ALREADY_COMMITTED, HttpStatus.CONFLICT);
        }
        RoommateMatchStatus from = match.getStatus();
        match.setStatus(RoommateMatchStatus.CHATTING);
        appendEvent(match, from, RoommateMatchStatus.CHATTING, actorUserId, "start chat");
        publish(match, from, RoommateMatchStatus.CHATTING, actorUserId, counterpartyId(match, actorUserId), null);
    }

    private void doScheduleViewing(RoommateMatch match, Long actorUserId, RoommateTransitionCommand cmd) {
        stateMachine.target(RoommateMatchAction.SCHEDULE_VIEWING, match.getStatus());
        if (cmd.viewingDate() == null) {
            throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_REQUIRED, HttpStatus.BAD_REQUEST);
        }
        if (cmd.viewingDate().isBefore(Instant.now())) {
            throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_IN_PAST, HttpStatus.BAD_REQUEST);
        }

        RoommateViewing viewing = RoommateViewing.builder()
            .match(match)
            .listingId(cmd.listingId())
            .address(cmd.address())
            .viewingDate(cmd.viewingDate())
            .createdByUserId(actorUserId)
            .status(RoommateViewingStatus.SCHEDULED)
            .build();
        if (cmd.listingId() != null) {
            applyListingSnapshot(viewing, requireListingCard(cmd.listingId()));
        }
        viewing = viewingRepository.save(viewing);

        RoommateMatchStatus from = match.getStatus();
        match.setStatus(RoommateMatchStatus.VIEWING_TOGETHER);
        appendEvent(match, from, RoommateMatchStatus.VIEWING_TOGETHER, actorUserId, "schedule viewing #" + viewing.getId());
        publish(match, from, RoommateMatchStatus.VIEWING_TOGETHER, actorUserId, counterpartyId(match, actorUserId), viewing.getId());
    }

    private void doCancelViewing(RoommateMatch match, Long actorUserId) {
        stateMachine.target(RoommateMatchAction.CANCEL_VIEWING, match.getStatus());
        viewingRepository.findFirstByMatchIdAndStatusOrderByViewingDateAsc(match.getId(), RoommateViewingStatus.SCHEDULED)
            .ifPresent(v -> {
                v.setStatus(RoommateViewingStatus.CANCELLED);
                viewingRepository.save(v);
            });
        RoommateMatchStatus from = match.getStatus();
        match.setStatus(RoommateMatchStatus.CHATTING);
        appendEvent(match, from, RoommateMatchStatus.CHATTING, actorUserId, "cancel viewing");
        publish(match, from, RoommateMatchStatus.CHATTING, actorUserId, counterpartyId(match, actorUserId), null);
    }

    private void doConfirmMoveIn(RoommateMatch match, Long actorUserId) {
        stateMachine.target(RoommateMatchAction.CONFIRM_MOVE_IN, match.getStatus());
        if (isLo(match, actorUserId)) {
            match.setLoMoved(true);
        } else {
            match.setHiMoved(true);
        }

        if (!(match.isLoMoved() && match.isHiMoved())) {
            appendEvent(match, match.getStatus(), match.getStatus(), actorUserId, "move-in confirmed by one side");
            publish(match, match.getStatus(), match.getStatus(), actorUserId, counterpartyId(match, actorUserId), null);
            return;
        }

        RoommateMatchStatus from = match.getStatus();
        match.setStatus(RoommateMatchStatus.MOVED_IN);
        match.setExpiresAt(null);
        releaseCommitment(match);
        archiveOtherActiveMatches(match);
        if (match.getScenario() == RoommateScenario.ROOM && match.getRoommateListing() != null) {
            match.getRoommateListing().setStatus(RoommateListingStatus.FILLED);
        }
        appendEvent(match, from, RoommateMatchStatus.MOVED_IN, actorUserId, "both confirmed move-in");
        publish(match, from, RoommateMatchStatus.MOVED_IN, actorUserId, counterpartyId(match, actorUserId), null);
    }

    private void archiveOtherActiveMatches(RoommateMatch keep) {
        for (Long userId : List.of(keep.getUserLoId(), keep.getUserHiId())) {
            List<RoommateMatch> others = matchRepository.findOtherActive(
                userId, keep.getScenario(), keep.getId(), ACTIVE_STATUSES);
            for (RoommateMatch other : others) {
                RoommateMatchStatus from = other.getStatus();
                other.setStatus(RoommateMatchStatus.ARCHIVED);
                other.setExpiresAt(null);
                releaseCommitment(other);
                matchRepository.save(other);
                appendEvent(other, from, RoommateMatchStatus.ARCHIVED, null, "archived: participant moved in elsewhere");
                publish(other, from, RoommateMatchStatus.ARCHIVED, null, counterpartyId(other, userId), null);
            }
        }
    }

    @Override
    @Transactional
    public int expireStaleMatches() {
        Instant now = Instant.now();
        List<RoommateMatch> candidates = matchRepository.findByStatusAndExpiresAtBefore(
            RoommateMatchStatus.WAITING_RESPONSE, now);
        int expired = 0;
        for (RoommateMatch candidate : candidates) {
            RoommateMatch match = matchRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (match == null || match.getStatus() != RoommateMatchStatus.WAITING_RESPONSE
                || match.getExpiresAt() == null || match.getExpiresAt().isAfter(now)) {
                continue;
            }
            match.setStatus(RoommateMatchStatus.EXPIRED);
            match.setExpiresAt(null);
            matchRepository.save(match);
            appendEvent(match, RoommateMatchStatus.WAITING_RESPONSE, RoommateMatchStatus.EXPIRED, null, "no response in time");
            publish(match, RoommateMatchStatus.WAITING_RESPONSE, RoommateMatchStatus.EXPIRED, null, match.getInitiatorUserId(), null);
            expired++;
        }
        if (expired > 0) {
            log.info("Expired {} stale roommate matches", expired);
        }
        return expired;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoommateMatchView> myMatches(Long userId, List<RoommateMatchStatus> statuses, Pageable pageable) {
        Page<RoommateMatchParticipant> page = (statuses == null || statuses.isEmpty())
            ? participantRepository.findMine(userId, pageable)
            : participantRepository.findMineByStatuses(userId, statuses, pageable);
        Map<Long, UserBriefResponse> briefs = findBriefs(page.getContent().stream()
            .map(p -> counterpartyId(p.getMatch(), userId))
            .distinct()
            .toList());
        return page.map(p -> buildView(p.getMatch(), userId, briefs));
    }

    @Override
    @Transactional(readOnly = true)
    public RoommateMatchView getMatch(Long matchId, Long requesterUserId) {
        RoommateMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MATCH_NOT_FOUND, HttpStatus.NOT_FOUND));
        assertParticipant(match, requesterUserId);
        return view(match, requesterUserId);
    }

    @Override
    @Transactional
    @UserAudit(value = UserEvent.ROOMMATE_CONTACT_REVEALED, userId = "#requesterUserId", params = {"#matchId", "#requesterUserId"})
    public RoommateContactResponse getContacts(Long matchId, Long requesterUserId) {
        RoommateMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MATCH_NOT_FOUND, HttpStatus.NOT_FOUND));
        assertParticipant(match, requesterUserId);
        if (match.getContactsUnlockedAt() == null) {
            throw new AppException(InternalExceptionCode.EX403_ROOMMATE_MATCH_NOT_UNLOCKED, HttpStatus.FORBIDDEN);
        }
        Long counterpartyId = counterpartyId(match, requesterUserId);
        String username = identityMsService.findUserBriefs(List.of(counterpartyId)).stream()
            .filter(brief -> counterpartyId.equals(brief.userId()))
            .findFirst()
            .map(UserBriefResponse::username)
            .orElse(null);
        return new RoommateContactResponse(counterpartyId, username, username == null ? null : "https://t.me/" + username);
    }

    private void assertNotSelf(Long a, Long b) {
        if (Objects.equals(a, b)) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_SELF_ACTION, HttpStatus.BAD_REQUEST);
        }
    }

    private void assertParticipant(RoommateMatch match, Long userId) {
        if (!isLo(match, userId) && !isHi(match, userId)) {
            throw new AppException(InternalExceptionCode.EX403_ROOMMATE_NOT_PARTICIPANT, HttpStatus.FORBIDDEN);
        }
    }

    private boolean isLo(RoommateMatch match, Long userId) {
        return match.getUserLoId().equals(userId);
    }

    private boolean isHi(RoommateMatch match, Long userId) {
        return match.getUserHiId().equals(userId);
    }

    private Long counterpartyId(RoommateMatch match, Long userId) {
        return isLo(match, userId) ? match.getUserHiId() : match.getUserLoId();
    }

    private void setDecision(RoommateMatch match, boolean actorIsLo, RoommateDecision decision) {
        if (actorIsLo) {
            match.setLoDecision(decision);
        } else {
            match.setHiDecision(decision);
        }
    }

    private void releaseCommitment(RoommateMatch match) {
        List<RoommateMatchParticipant> participants = participantRepository.findByMatchId(match.getId());
        participants.forEach(p -> p.setCommitted(false));
        participantRepository.saveAll(participants);
    }

    private void saveParticipant(RoommateMatch match, Long userId, RoommateScenario scenario, RoommateParticipantRole role) {
        participantRepository.save(RoommateMatchParticipant.builder()
            .match(match)
            .userId(userId)
            .scenario(scenario)
            .role(role)
            .committed(false)
            .build());
    }

    private void appendEvent(
        RoommateMatch match, RoommateMatchStatus from, RoommateMatchStatus to, Long actorUserId, String note) {
        matchEventRepository.save(RoommateMatchEvent.builder()
            .match(match)
            .fromStatus(from)
            .toStatus(to)
            .actorUserId(actorUserId)
            .note(note)
            .build());
    }

    private void publish(
        RoommateMatch match, RoommateMatchStatus from, RoommateMatchStatus to,
        Long actorUserId, Long counterpartyUserId, Long roommateViewingId) {
        eventPublisher.publishEvent(new RoommateMatchTransitionEvent(
            match.getId(), match.getScenario(), from, to, actorUserId,
            List.of(match.getUserLoId(), match.getUserHiId()), counterpartyUserId, roommateViewingId));
    }

    private ListingDto requireListingCard(Long listingId) {
        return listingMsService.findCards(List.of(listingId)).stream()
            .filter(card -> listingId.equals(card.getId()))
            .findFirst()
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private static void applyListingSnapshot(RoommateViewing viewing, ListingDto listing) {
        viewing.setListingTitle(listing.getTitle());
        viewing.setListingStreet(listing.getStreet());
        viewing.setListingMetroStation(listing.getMetroStation());
        viewing.setListingPrice(listing.getPrice());
        viewing.setListingCurrency(listing.getCurrency());
        viewing.setListingCoverPhotoUrl(listing.getPreviewPhoto());
    }

    private RoommateMatchView view(RoommateMatch match, Long requesterUserId) {
        Long counterpartyId = counterpartyId(match, requesterUserId);
        return buildView(match, requesterUserId, findBriefs(List.of(counterpartyId)));
    }

    private RoommateMatchView buildView(RoommateMatch match, Long requesterUserId, Map<Long, UserBriefResponse> briefs) {
        boolean isLo = isLo(match, requesterUserId);
        Long counterpartyId = counterpartyId(match, requesterUserId);
        UserBriefResponse counterparty = briefs.get(counterpartyId);
        RoommateDecision myDecision = isLo ? match.getLoDecision() : match.getHiDecision();
        RoommateDecision counterpartyDecision = isLo ? match.getHiDecision() : match.getLoDecision();
        boolean iConfirmed = isLo ? match.isLoMoved() : match.isHiMoved();
        boolean counterpartyConfirmed = isLo ? match.isHiMoved() : match.isLoMoved();

        RoommateViewing activeViewing = viewingRepository
            .findFirstByMatchIdAndStatusOrderByViewingDateAsc(match.getId(), RoommateViewingStatus.SCHEDULED)
            .orElse(null);

        return new RoommateMatchView(
            match.getId(),
            match.getScenario(),
            match.getStatus(),
            match.getStatusVersion() == null ? 0L : match.getStatusVersion(),
            counterpartyId,
            counterparty == null ? null : counterparty.username(),
            counterparty == null ? null : counterparty.photoUrl(),
            match.getRoommateListing() == null ? null : match.getRoommateListing().getId(),
            myDecision,
            counterpartyDecision,
            Objects.equals(match.getInitiatorUserId(), requesterUserId),
            match.getContactsUnlockedAt() != null,
            iConfirmed,
            counterpartyConfirmed,
            activeViewing == null ? null : activeViewing.getId(),
            activeViewing == null ? null : activeViewing.getViewingDate(),
            activeViewing == null ? null : activeViewing.getStatus(),
            match.getExpiresAt(),
            match.getCreatedAt(),
            match.getUpdatedAt());
    }

    private Map<Long, UserBriefResponse> findBriefs(Collection<Long> userIds) {
        try {
            return identityMsService.findUserBriefs(userIds).stream()
                .collect(Collectors.toMap(UserBriefResponse::userId, Function.identity()));
        } catch (RuntimeException e) {
            log.warn("Roommate matches are shown without names and photos: {}", e.getMessage());
            return Map.of();
        }
    }
}
