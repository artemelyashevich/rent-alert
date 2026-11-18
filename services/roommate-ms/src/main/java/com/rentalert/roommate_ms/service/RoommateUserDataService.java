package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.repository.RoommateUserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateUserDataService {

    private final RoommateUserDataRepository repository;

    @Transactional
    public void deleteUserData(Long userId) {
        repository.deleteMatchesOfUser(userId);
        repository.deleteHiddenOfUser(userId);
        repository.deleteModerationTasksOfUser(userId);
        repository.deleteListingsOfUser(userId);
        repository.deleteProfileOfUser(userId);
    }
}
