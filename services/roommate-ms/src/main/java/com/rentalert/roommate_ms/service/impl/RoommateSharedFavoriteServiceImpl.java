package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.roommate_ms.domain.RoommateSharedFavorite;
import com.rentalert.roommate_ms.domain.RoommateSharedFavoriteEmbeddedId;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteCreateDto;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteResponse;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteUpdateDto;
import com.rentalert.roommate_ms.mapper.RoommateSharedFavoriteMapper;
import com.rentalert.roommate_ms.repository.RoommateSharedFavoriteRepository;
import com.rentalert.roommate_ms.service.RoommateMatchAccess;
import com.rentalert.roommate_ms.service.RoommateSharedFavoriteService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateSharedFavoriteServiceImpl implements RoommateSharedFavoriteService {

    private final ListingMsService listingMsService;
    private final RoommateMatchAccess matchAccess;
    private final RoommateSharedFavoriteMapper mapper;
    private final RoommateSharedFavoriteRepository favoriteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoommateSharedFavoriteResponse> findAll(Long matchId, Long requesterUserId) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        List<RoommateSharedFavorite> favorites = favoriteRepository.findAllByIdMatchId(matchId);
        Map<Long, ListingDto> cards = findCardsOrEmpty(favorites.stream().map(f -> f.getId().getListingId()).toList());
        return favorites.stream()
            .map(f -> mapper.toResponse(f, cards.get(f.getId().getListingId())))
            .toList();
    }

    @Override
    @Transactional
    public RoommateSharedFavoriteResponse add(Long matchId, Long requesterUserId, RoommateSharedFavoriteCreateDto request) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        RoommateSharedFavoriteEmbeddedId id = new RoommateSharedFavoriteEmbeddedId(matchId, request.listingId());
        if (favoriteRepository.existsById(id)) {
            throw alreadyExists();
        }
        ListingDto listing = requireListingCard(request.listingId());

        RoommateSharedFavorite favorite = RoommateSharedFavorite.builder()
            .id(id)
            .listingStatus(request.listingStatus())
            .viewingDate(request.viewingDate())
            .addedByUserId(requesterUserId)
            .build();
        try {
            favorite = favoriteRepository.saveAndFlush(favorite);
        } catch (DataIntegrityViolationException race) {
            throw alreadyExists();
        }
        log.info("Shared favorite added. matchId={}, listingId={}", matchId, request.listingId());
        return mapper.toResponse(favorite, listing);
    }

    @Override
    @Transactional
    public RoommateSharedFavoriteResponse updateStatus(
        Long matchId, Long requesterUserId, Long listingId, RoommateSharedFavoriteUpdateDto request) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        RoommateSharedFavorite favorite = requireShared(matchId, listingId);
        favorite.setListingStatus(request.listingStatus());
        favorite.setViewingDate(request.viewingDate());
        favorite = favoriteRepository.save(favorite);
        ListingDto listing = findCardsOrEmpty(List.of(listingId)).get(listingId);
        return mapper.toResponse(favorite, listing);
    }

    @Override
    @Transactional
    public void remove(Long matchId, Long requesterUserId, Long listingId) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        favoriteRepository.delete(requireShared(matchId, listingId));
    }

    private RoommateSharedFavorite requireShared(Long matchId, Long listingId) {
        return favoriteRepository.findById(new RoommateSharedFavoriteEmbeddedId(matchId, listingId))
            .orElseThrow(() -> new AppException(
                InternalExceptionCode.EX404_ROOMMATE_SHARED_FAVORITE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private ListingDto requireListingCard(Long listingId) {
        return listingMsService.findCards(List.of(listingId)).stream()
            .filter(card -> listingId.equals(card.getId()))
            .findFirst()
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private Map<Long, ListingDto> findCardsOrEmpty(Collection<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        try {
            return listingMsService.findCards(listingIds).stream()
                .collect(Collectors.toMap(ListingDto::getId, Function.identity(), (a, b) -> a));
        } catch (RuntimeException e) {
            log.warn("Shared favorites are shown without listing cards: {}", e.getMessage());
            return Map.of();
        }
    }

    private static AppException alreadyExists() {
        return new AppException(InternalExceptionCode.EX400_ROOMMATE_SHARED_FAVORITE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
    }
}
