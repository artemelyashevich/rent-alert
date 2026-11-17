package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.dto.RoommateListingResponse;
import com.rentalert.roommate_ms.mapper.RoommateListingMapper;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.service.RoommateListingService;
import com.rentalert.roommate_ms.service.RoommateModerationGate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateListingServiceImpl implements RoommateListingService {

    private static final Set<RoommateListingStatus> OWNER_SETTABLE_STATUSES =
        Set.of(RoommateListingStatus.ACTIVE, RoommateListingStatus.PAUSED, RoommateListingStatus.ARCHIVED);

    private final RoommateListingRepository listingRepository;
    private final ListingMsService listingMsService;
    private final RoommateListingMapper listingMapper;
    private final RoommateModerationGate moderationGate;

    @Override
    @Transactional
    public RoommateListingResponse create(Long authorUserId, RoommateListingRequest request) {
        requireLinkedListingExists(request.getListingId());
        RoommateListing entity = RoommateListing.builder()
            .authorUserId(authorUserId)
            .status(RoommateListingStatus.DRAFT)
            .build();
        listingMapper.update(entity, request);
        entity = listingRepository.save(entity);

        applyModeration(entity, null);
        entity.setStatus(isDescriptionApproved(entity) ? RoommateListingStatus.ACTIVE : RoommateListingStatus.PENDING_MODERATION);
        RoommateListing saved = saveRespectingActiveConstraint(entity);
        log.info("Roommate listing created. id={}, authorUserId={}", saved.getId(), authorUserId);
        return listingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoommateListingResponse update(Long listingId, Long authorUserId, RoommateListingRequest request) {
        RoommateListing entity = requireOwned(listingId, authorUserId);
        if (!Objects.equals(entity.getListingId(), request.getListingId())) {
            requireLinkedListingExists(request.getListingId());
        }
        String previousDescription = entity.getDescriptionSubmitted();
        listingMapper.update(entity, request);
        applyModeration(entity, previousDescription);
        return listingMapper.toResponse(listingRepository.save(entity));
    }

    @Override
    @Transactional
    public RoommateListingResponse setStatus(Long listingId, Long authorUserId, RoommateListingStatus status) {
        if (!OWNER_SETTABLE_STATUSES.contains(status)) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_LISTING_STATUS_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        }
        RoommateListing entity = requireOwned(listingId, authorUserId);
        if (status == RoommateListingStatus.ACTIVE && !isDescriptionApproved(entity)) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_DESCRIPTION_NOT_APPROVED, HttpStatus.BAD_REQUEST);
        }
        entity.setStatus(status);
        RoommateListing saved = status == RoommateListingStatus.ACTIVE
            ? saveRespectingActiveConstraint(entity)
            : listingRepository.save(entity);
        return listingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoommateListingResponse> findMine(Long authorUserId, Pageable pageable) {
        return listingRepository.findAllByAuthorUserId(authorUserId, pageable).map(listingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoommateListingResponse getOwned(Long listingId, Long authorUserId) {
        return listingMapper.toResponse(requireOwned(listingId, authorUserId));
    }

    private RoommateListing requireOwned(Long listingId, Long authorUserId) {
        RoommateListing entity = listingRepository.findById(listingId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!entity.getAuthorUserId().equals(authorUserId)) {
            throw new AppException(InternalExceptionCode.EX403_ROOMMATE_LISTING_NOT_OWNER, HttpStatus.FORBIDDEN);
        }
        return entity;
    }

    private void requireLinkedListingExists(Long linkedListingId) {
        if (linkedListingId != null && listingMsService.findCards(List.of(linkedListingId)).isEmpty()) {
            throw new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    private RoommateListing saveRespectingActiveConstraint(RoommateListing entity) {
        try {
            return listingRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException alreadyHaveActive) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_LISTING_ALREADY_ACTIVE, HttpStatus.CONFLICT);
        }
    }

    private static boolean isDescriptionApproved(RoommateListing entity) {
        ModerationStatus status = entity.getDescriptionModStatus();
        return status == ModerationStatus.APPROVED_AUTO || status == ModerationStatus.APPROVED;
    }

    private void applyModeration(RoommateListing entity, String previousDescription) {
        if (previousDescription != null && Objects.equals(entity.getDescriptionSubmitted(), previousDescription)) {
            return;
        }
        ModerationStatus status = moderationGate.moderate(
            ModerationSubjectType.LISTING, entity.getId(), "description", entity.getDescriptionSubmitted());
        entity.setDescriptionModStatus(status);
        if (status == ModerationStatus.APPROVED_AUTO || status == ModerationStatus.APPROVED) {
            entity.setDescriptionApproved(entity.getDescriptionSubmitted());
        }
    }
}
