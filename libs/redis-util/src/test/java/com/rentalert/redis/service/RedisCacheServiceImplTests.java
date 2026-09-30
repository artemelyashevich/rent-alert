package com.rentalert.redis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.impl.RedisCacheServiceImpl;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisCacheServiceImplTests {

    private final Map<String, String> store = new HashMap<>();

    private CacheService cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        doAnswer(inv -> store.put(inv.getArgument(0), inv.getArgument(1)))
            .when(ops).set(anyString(), anyString(), any(Duration.class));
        when(ops.get(anyString())).thenAnswer(inv -> store.get((String) inv.getArgument(0)));
        when(template.delete(anyString())).thenAnswer(inv -> store.remove((String) inv.getArgument(0)) != null);
        cacheService = new RedisCacheServiceImpl(template);
    }

    @Test
    void putThenGetReturnsValueOfCacheTypeUnderPrefixedKey() {
        cacheService.put(CacheType.USER_SESSION, "token", UserSession.builder().userId(5L).username("ann").build());

        assertThat(store).containsKey("USER_SESSION:token");
        Optional<UserSession> cached = cacheService.get(CacheType.USER_SESSION, "token");
        assertThat(cached).get().satisfies(session -> {
            assertThat(session.getUserId()).isEqualTo(5L);
            assertThat(session.getUsername()).isEqualTo("ann");
        });
    }

    @Test
    void stringValuesRoundTrip() {
        cacheService.put(CacheType.USER_ACTIVE_TOKEN, 5, "jwt");

        Optional<String> token = cacheService.get(CacheType.USER_ACTIVE_TOKEN, 5);
        assertThat(token).contains("jwt");
    }

    @Test
    void loaderIsUsedOnMissAndResultCached() {
        String loaded = cacheService.get(CacheType.USER_ACTIVE_TOKEN, 7, () -> "fresh");

        assertThat(loaded).isEqualTo("fresh");
        assertThat(store).containsKey("USER_ACTIVE_TOKEN:7");
    }

    @Test
    void loaderIsSkippedOnHit() {
        cacheService.put(CacheType.USER_ACTIVE_TOKEN, 7, "cached");

        String value = cacheService.get(CacheType.USER_ACTIVE_TOKEN, 7, () -> {
            throw new AssertionError("loader must not be called on a hit");
        });

        assertThat(value).isEqualTo("cached");
    }

    @Test
    void unreadableEntryIsTreatedAsMissAndEvicted() {
        store.put("USER_SESSION:token", "{not json");

        Optional<UserSession> cached = cacheService.get(CacheType.USER_SESSION, "token");

        assertThat(cached).isEmpty();
        assertThat(store).doesNotContainKey("USER_SESSION:token");
    }
}
