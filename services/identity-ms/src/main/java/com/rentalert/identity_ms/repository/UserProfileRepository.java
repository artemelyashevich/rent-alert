package com.rentalert.identity_ms.repository;

import com.rentalert.identity_ms.domain.UserProfile;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;


public interface UserProfileRepository extends JpaRepository<@NonNull UserProfile, @NonNull Long>, JpaSpecificationExecutor<UserProfile> {

    boolean existsByTelegramIdHash(String telegramIdHash);

    Optional<UserProfile> findByUsername(String username);

    @Query("""
            select u
            from UserProfile u
            left join fetch u.filters
            left join fetch u.settings
            where u.telegramIdHash = :telegramIdHash
        """)
    Optional<UserProfile> findByTelegramIdHash(String telegramIdHash);
}
