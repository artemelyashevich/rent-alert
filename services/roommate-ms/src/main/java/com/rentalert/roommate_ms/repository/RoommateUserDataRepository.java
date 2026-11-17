package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateProfile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RoommateUserDataRepository extends Repository<RoommateProfile, Long> {

    @Modifying
    @Query(value = "DELETE FROM roommate_matches WHERE user_lo_id = :userId OR user_hi_id = :userId", nativeQuery = true)
    int deleteMatchesOfUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
        DELETE FROM roommate_hidden
        WHERE user_id = :userId
           OR (subject_type = 'ROOMMATE_PROFILE' AND subject_id = :userId)
           OR (subject_type = 'ROOMMATE_LISTING' AND subject_id IN (
               SELECT id FROM roommate_listings WHERE author_user_id = :userId))
        """, nativeQuery = true)
    int deleteHiddenOfUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
        DELETE FROM roommate_moderation_tasks
        WHERE (subject_type = 'PROFILE' AND subject_id = :userId)
           OR (subject_type = 'LISTING' AND subject_id IN (
               SELECT id FROM roommate_listings WHERE author_user_id = :userId))
        """, nativeQuery = true)
    int deleteModerationTasksOfUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM roommate_listings WHERE author_user_id = :userId", nativeQuery = true)
    int deleteListingsOfUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM roommate_profiles WHERE user_id = :userId", nativeQuery = true)
    int deleteProfileOfUser(@Param("userId") Long userId);
}
