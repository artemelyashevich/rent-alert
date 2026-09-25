package com.rentalert.redis.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		cacheService = new RedisCacheServiceImpl(template);
	}

	@Test
	void putThenGetReturnsSameValueUnderPrefixedKey() {
		var value = new Listing("flat", 3);

		cacheService.put("listings", 42, value);

		assertThat(store).containsKey("listings:42");
		assertThat(cacheService.get("listings", 42, Listing.class)).contains(value);
	}

	@Test
	void loaderIsUsedOnMissAndResultCached() {
		var loaded = cacheService.get("listings", 7, Listing.class, () -> new Listing("room", 1));

		assertThat(loaded).isEqualTo(new Listing("room", 1));
		assertThat(store).containsKey("listings:7");
	}

	record Listing(String name, int rooms) {
	}
}
