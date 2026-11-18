package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoommateUserBriefsTests {

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final InMemoryCache cache = new InMemoryCache();
    private final RoommateUserBriefs briefs = new RoommateUserBriefs(identityMsService, new RoommateBatchCache(cache.service()));

    @Test
    void repeatedLookupIsServedFromCache() {
        when(identityMsService.findUserBriefs(List.of(1L))).thenReturn(List.of(brief(1L)));

        briefs.find(List.of(1L));
        var second = briefs.find(List.of(1L));

        assertThat(second).containsEntry(1L, brief(1L));
        verify(identityMsService, times(1)).findUserBriefs(anyCollection());
    }

    @Test
    void onlyMissingUsersAreRequestedFromIdentity() {
        when(identityMsService.findUserBriefs(List.of(1L))).thenReturn(List.of(brief(1L)));
        when(identityMsService.findUserBriefs(List.of(2L))).thenReturn(List.of(brief(2L)));
        briefs.find(List.of(1L));

        var result = briefs.find(List.of(1L, 2L));

        assertThat(result).containsOnlyKeys(1L, 2L);
        verify(identityMsService).findUserBriefs(List.of(2L));
    }

    @Test
    void cachedBriefsAreServedWhileIdentityIsDown() {
        when(identityMsService.findUserBriefs(List.of(1L))).thenReturn(List.of(brief(1L)));
        briefs.find(List.of(1L));
        when(identityMsService.findUserBriefs(List.of(2L))).thenThrow(new IllegalStateException("identity-ms is down"));

        var result = briefs.find(List.of(1L, 2L));

        assertThat(result).containsOnlyKeys(1L);
    }

    @Test
    void identityIsUsedWhenTheCacheIsUnreachable() {
        cache.breakDown();
        when(identityMsService.findUserBriefs(List.of(1L))).thenReturn(List.of(brief(1L)));

        assertThat(briefs.find(List.of(1L))).containsEntry(1L, brief(1L));
    }

    @Test
    void usersUnknownToIdentityAreNotCached() {
        when(identityMsService.findUserBriefs(List.of(3L))).thenReturn(List.of());

        briefs.find(List.of(3L));
        briefs.find(List.of(3L));

        verify(identityMsService, times(2)).findUserBriefs(List.of(3L));
    }

    private static UserBriefResponse brief(long userId) {
        return new UserBriefResponse(userId, "user" + userId, "photo" + userId + ".jpg");
    }
}
