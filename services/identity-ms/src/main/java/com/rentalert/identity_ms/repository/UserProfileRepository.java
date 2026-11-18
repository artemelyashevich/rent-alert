package com.rentalert.identity_ms.repository;

import com.rentalert.identity_ms.domain.UserProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UserProfileRepository extends JpaRepository<@NonNull UserProfile, @NonNull Long>, JpaSpecificationExecutor<UserProfile> {

    boolean existsByTelegramIdHash(String telegramIdHash);

    Optional<UserProfile> findByUsername(String username);

    List<UserProfile> findAllByUsernameContainingIgnoreCaseOrderByIdAsc(String username);

    @Query("""
            select u
            from UserProfile u
            left join fetch u.filters
            left join fetch u.settings
            where u.telegramIdHash = :telegramIdHash
        """)
    Optional<UserProfile> findByTelegramIdHash(String telegramIdHash);

    @Query("""
            select u
            from UserProfile u
            join fetch u.settings s
            where s.roleName <> com.rentalert.dto.profile.UserRole.ADMIN
        """)
    List<UserProfile> findAllNonAdmins();

    @Query("""
            select u
            from UserProfile u
            join fetch u.settings s
            where s.roleName <> com.rentalert.dto.profile.UserRole.ADMIN
              and u.id in :ids
        """)
    List<UserProfile> findNonAdminsByIdIn(@Param("ids") Collection<Long> ids);
}
