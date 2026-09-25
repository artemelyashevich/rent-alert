package com.rentalert.redis.service;

import com.rentalert.common.auth.JwtService;
import com.rentalert.common.auth.JwtServiceImpl;
import com.rentalert.common.auth.JwtType;
import com.rentalert.common.config.AuthProperties;
import com.rentalert.common.exception.AuthException;
import com.rentalert.dto.user.UserSession;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSessionServiceImplTests {

	private final Map<String, Object> redis = new HashMap<>();
	private final JwtService jwt = new JwtServiceImpl(new AuthProperties(
			"test-access-secret-that-is-at-least-32-bytes", "test-refresh-secret-that-is-at-least-32-bytes", 3600));
	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private UserSessionService sessions;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		CacheService cache = mock(CacheService.class);
		doAnswer(inv -> redis.put(inv.getArgument(0) + ":" + inv.getArgument(1), inv.getArgument(2)))
				.when(cache).put(anyString(), any(), any(), any(Duration.class));
		doAnswer(inv -> redis.remove(inv.getArgument(0) + ":" + inv.getArgument(1)))
				.when(cache).evict(anyString(), any());
		when(cache.get(anyString(), any(), any(Class.class)))
				.thenAnswer(inv -> Optional.ofNullable(redis.get(inv.getArgument(0) + ":" + inv.getArgument(1))));
		sessions = new UserSessionServiceImpl(cache, request, jwt);
	}

	@Test
	void createdSessionIsFoundByAuthorizationHeader() {
		String token = jwt.generateToken(5L, JwtType.ACCESS);

		sessions.createSession(token, UserSession.builder().userId(5L).username("ann").email("a@b.c").build());
		request.addHeader("Authorization", "Bearer " + token);

		UserSession current = sessions.getCurrentUser();
		assertThat(current.getUserId()).isEqualTo(5L);
		assertThat(current.getUsername()).isEqualTo("ann");
		assertThat(current.getToken()).isEqualTo(token);
		assertThat(redis).containsEntry("USER_ACTIVE_TOKEN:5", token);
	}

	@Test
	void newLoginReplacesPreviousSessionOfSameUser() throws InterruptedException {
		String first = jwt.generateToken(5L, JwtType.ACCESS);
		sessions.createSession(first, UserSession.builder().userId(5L).build());
		Thread.sleep(1100); // JWT timestamps have second precision: make the second token differ
		String second = jwt.generateToken(5L, JwtType.ACCESS);

		sessions.createSession(second, UserSession.builder().userId(5L).build());

		assertThat(redis).doesNotContainKey("USER_SESSION:" + first).containsKey("USER_SESSION:" + second);
	}

	@Test
	void noSessionMeansAuthException() {
		request.addHeader("Authorization", "Bearer " + jwt.generateToken(9L, JwtType.ACCESS));

		assertThatThrownBy(sessions::getCurrentUser).isInstanceOf(AuthException.class);
	}
}
