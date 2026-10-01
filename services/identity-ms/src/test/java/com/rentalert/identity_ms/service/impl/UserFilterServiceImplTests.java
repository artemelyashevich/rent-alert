package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.mapper.UserFilterMapper;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.repository.UserFilterRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserFilterServiceImplTests {

    private final UserFilterRepository repository = mock(UserFilterRepository.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final UserFilterMapper mapper = mock(UserFilterMapper.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final UserFilterServiceImpl service = new UserFilterServiceImpl(
        repository, userProfileService, mapper, userSessionService, userEventPublisher);

    @BeforeEach
    void currentUser() {
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(5L).build());
    }

    @Test
    void createdFilterIsSentToStatistics() {
        UserFilterRequest request = new UserFilterRequest();
        UserFilter saved = UserFilter.builder().build();
        saved.setId(9L);
        when(mapper.toEntity(request)).thenReturn(UserFilter.builder().build());
        when(repository.save(any())).thenReturn(saved);

        service.create(request);

        verify(userEventPublisher).publish(UserEvent.FILTER_CREATED, 5L, 9L);
    }

    @Test
    void updatedFilterIsSentToStatistics() {
        UserFilter filter = UserFilter.builder().build();
        when(repository.findByIdAndUserProfileId(9L, 5L)).thenReturn(Optional.of(filter));
        when(repository.save(filter)).thenReturn(filter);

        service.update(9L, new UserFilterRequest());

        verify(userEventPublisher).publish(UserEvent.FILTER_UPDATED, 5L, 9L);
    }

    @Test
    void activatedFilterIsSentToStatistics() {
        UserFilter filter = UserFilter.builder().build();
        when(repository.findByIdAndUserProfileId(9L, 5L)).thenReturn(Optional.of(filter));
        when(repository.save(filter)).thenReturn(filter);

        service.setActivation(9L, true);

        verify(userEventPublisher).publish(UserEvent.FILTER_ACTIVATED, 5L, 9L);
    }

    @Test
    void deactivationIsNotSent() {
        UserFilter filter = UserFilter.builder().build();
        when(repository.findByIdAndUserProfileId(9L, 5L)).thenReturn(Optional.of(filter));
        when(repository.save(filter)).thenReturn(filter);

        service.setActivation(9L, false);

        verify(userEventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void deletedFilterIsSentToStatistics() {
        when(repository.findByIdAndUserProfileId(9L, 5L)).thenReturn(Optional.of(UserFilter.builder().active(false).build()));

        service.delete(9L);

        verify(userEventPublisher).publish(UserEvent.FILTER_DELETED, 5L, 9L);
    }

    @Test
    void activeFilterDeletionIsNotSent() {
        when(repository.findByIdAndUserProfileId(9L, 5L)).thenReturn(Optional.of(UserFilter.builder().active(true).build()));

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(AppBadRequestException.class);
        verify(userEventPublisher, never()).publish(any(), any(), any());
    }
}
