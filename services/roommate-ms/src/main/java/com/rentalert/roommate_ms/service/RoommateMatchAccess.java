package com.rentalert.roommate_ms.service;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateMatchAccess {

    private final RoommateMatchRepository matchRepository;

    public RoommateMatch requireUnlocked(Long matchId, Long userId) {
        RoommateMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MATCH_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!match.getUserLoId().equals(userId) && !match.getUserHiId().equals(userId)) {
            throw new AppException(InternalExceptionCode.EX403_ROOMMATE_NOT_PARTICIPANT, HttpStatus.FORBIDDEN);
        }
        if (match.getContactsUnlockedAt() == null) {
            throw new AppException(InternalExceptionCode.EX403_ROOMMATE_MATCH_NOT_UNLOCKED, HttpStatus.FORBIDDEN);
        }
        return match;
    }
}
