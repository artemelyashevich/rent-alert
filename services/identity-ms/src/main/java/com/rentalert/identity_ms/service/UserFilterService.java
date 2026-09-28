package com.rentalert.identity_ms.service;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.domain.UserProfile;
import java.util.List;
import java.util.Optional;

public interface UserFilterService {

    UserFilterResponse create(UserFilterRequest request);

    UserFilter createDefault(UserProfile userProfile);

    UserFilterResponse update(Long filterId, UserFilterRequest request);

    UserFilterResponse setActivation(Long filterId, boolean active);

    UserFilterResponse findById(Long filterId);

    List<UserFilterResponse> findAll();

    List<UserFilterResponse> findAllActive();

    void delete(Long filterId);

    UserFilter findActiveFilterByUserId(Long userId);

    /** Общий фильтр, принадлежащий не пользователю, а roommate-матчу (см. {@code UserFilter.ownerMatchId}). */
    Optional<UserFilterResponse> findByOwnerMatchId(Long matchId);

    /** Создаёт или обновляет общий фильтр roommate-матча. */
    UserFilterResponse upsertForMatch(Long matchId, UserFilterRequest request);

    /** Включает/выключает общий фильтр roommate-матча. */
    UserFilterResponse setActivationForMatch(Long matchId, boolean active);
}