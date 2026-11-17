package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.repository.RoommateUserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateUserDataService {

    private final RoommateUserDataRepository repository;

    /**
     * Deletes everything roommate-ms keeps for the user in one transaction: matches the user is part of (with their
     * participants, events, viewings and shared favorites), hidden records and moderation tasks about the user or
     * their listings, the user's listings, and the profile. Matches are removed first, so no foreign key blocks the rest.
     */
    @Transactional
    public void deleteUserData(Long userId) {
        repository.deleteMatchesOfUser(userId);
        repository.deleteHiddenOfUser(userId);
        repository.deleteModerationTasksOfUser(userId);
        repository.deleteListingsOfUser(userId);
        repository.deleteProfileOfUser(userId);
    }
}
