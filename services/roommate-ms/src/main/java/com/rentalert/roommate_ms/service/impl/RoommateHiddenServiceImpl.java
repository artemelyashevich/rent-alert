package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.roommate_ms.domain.RoommateHidden;
import com.rentalert.roommate_ms.domain.RoommateHiddenEmbeddedId;
import com.rentalert.roommate_ms.domain.RoommateHiddenSubjectType;
import com.rentalert.roommate_ms.repository.RoommateHiddenRepository;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.service.RoommateHiddenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateHiddenServiceImpl implements RoommateHiddenService {

    private final RoommateHiddenRepository hiddenRepository;
    private final RoommateListingRepository listingRepository;

    @Override
    @Transactional
    public void skipCandidate(Long viewerId, Long candidateUserId) {
        if (viewerId.equals(candidateUserId)) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_SELF_ACTION, HttpStatus.BAD_REQUEST);
        }
        hide(viewerId, RoommateHiddenSubjectType.ROOMMATE_PROFILE, candidateUserId);
    }

    @Override
    @Transactional
    public void skipListing(Long viewerId, Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new AppException(InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        hide(viewerId, RoommateHiddenSubjectType.ROOMMATE_LISTING, listingId);
    }

    private void hide(Long viewerId, RoommateHiddenSubjectType subjectType, Long subjectId) {
        RoommateHiddenEmbeddedId id = new RoommateHiddenEmbeddedId(viewerId, subjectType, subjectId);
        if (!hiddenRepository.existsById(id)) {
            hiddenRepository.save(RoommateHidden.builder().id(id).build());
        }
    }
}
