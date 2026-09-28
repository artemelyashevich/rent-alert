package com.rentalert.identity_ms.repository;

import com.rentalert.identity_ms.domain.Feedback;
import com.rentalert.dto.profile.FeedbackSubjectType;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepository extends JpaRepository<@NonNull Feedback, @NonNull Long> {

    @Query("""
            select f
            from Feedback f
            left join fetch f.userProfile
            order by f.createdAt desc
        """)
    Page<Feedback> findAllWithUser(Pageable pageable);

    boolean existsByUserProfileIdAndSubjectTypeAndSubjectId(Long userProfileId, FeedbackSubjectType subjectType, Long subjectId);
}
