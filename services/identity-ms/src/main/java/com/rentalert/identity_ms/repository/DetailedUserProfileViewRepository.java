package com.rentalert.identity_ms.repository;

import com.rentalert.identity_ms.domain.DetailedUserProfileView;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DetailedUserProfileViewRepository
    extends JpaRepository<@NonNull DetailedUserProfileView, @NonNull Long>,
            JpaSpecificationExecutor<DetailedUserProfileView> {
}
