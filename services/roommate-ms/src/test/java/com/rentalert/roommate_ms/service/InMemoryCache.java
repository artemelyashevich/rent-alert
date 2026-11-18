package com.rentalert.roommate_ms.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.redis.service.CacheService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.mockito.invocation.InvocationOnMock;

final class InMemoryCache {

    private final Map<String, Object> entries = new HashMap<>();
    private boolean brokenDown;

    CacheService service() {
        CacheService cache = mock(CacheService.class);
        when(cache.get(any(), any())).thenAnswer(invocation -> {
            failIfBrokenDown();
            return Optional.ofNullable(entries.get(keyOf(invocation)));
        });
        doAnswer(invocation -> {
            failIfBrokenDown();
            entries.put(keyOf(invocation), invocation.getArgument(2));
            return null;
        }).when(cache).put(any(), any(), any(), any());
        doAnswer(invocation -> {
            failIfBrokenDown();
            entries.remove(keyOf(invocation));
            return null;
        }).when(cache).evict(any(), any());
        return cache;
    }

    void breakDown() {
        brokenDown = true;
    }

    private void failIfBrokenDown() {
        if (brokenDown) {
            throw new IllegalStateException("redis is unreachable");
        }
    }

    private static String keyOf(InvocationOnMock invocation) {
        return invocation.getArgument(0) + ":" + invocation.getArgument(1);
    }
}
