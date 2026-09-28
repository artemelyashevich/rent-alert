package com.rentalert.redis.service;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtServiceImpl;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.common.config.AuthProperties;
import com.rentalert.common.exception.AuthException;
import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.common.security.AuthenticatedUser;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.impl.SessionAuthenticationService;
import com.rentalert.redis.service.impl.UserSessionServiceImpl;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionAuthenticationServiceTests {

	private final Map<String, Object> redis = new HashMap<>();
	private final JwtService jwt = new JwtServiceImpl(new AuthProperties(
			"test-access-secret-that-is-at-least-32-bytes", "test-refresh-secret-that-is-at-least-32-bytes", 3600));
	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private UserSessionService sessions;
	private SessionAuthenticationService authentication;

	@BeforeEach
	void setUp() {
		CacheService cache = mock(CacheService.class);
		doAnswer(inv -> redis.put(inv.getArgument(0) + ":" + inv.getArgument(1), inv.getArgument(2)))
				.when(cache).put(any(CacheType.class), any(), any(), any(Duration.class));
		doAnswer(inv -> redis.remove(inv.getArgument(0) + ":" + inv.getArgument(1)))
				.when(cache).evict(any(CacheType.class), any());
		when(cache.get(any(CacheType.class), any()))
				.thenAnswer(inv -> Optional.ofNullable(redis.get(inv.getArgument(0) + ":" + inv.getArgument(1))));
		sessions = new UserSessionServiceImpl(cache, request, jwt);
		authentication = new SessionAuthenticationService(jwt, sessions);
	}

	@Test
	void authenticatesTokenWithActiveSession() {
		String token = login(5L, "ADMIN");

		UserDetails user = authentication.findUserByToken(token);

		assertThat(user).isEqualTo(new AuthenticatedUser(5L, "ann", "a@b.c", "ADMIN"));
		assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
	}

	@Test
	void rejectsTokenWithoutSession() {
		String token = jwt.generateToken(user(5L, "USER"), JwtType.ACCESS);
		sendWith(token);

		assertThatThrownBy(() -> authentication.findUserByToken(token)).isInstanceOf(AuthException.class);
	}

	@Test
	void rejectsTokenOfInvalidatedSession() {
		String token = login(5L, "USER");
		sessions.invalidateSession(5L);

		assertThatThrownBy(() -> authentication.findUserByToken(token)).isInstanceOf(AuthException.class);
	}

	@Test
	void rejectsTokenSignedWithAnotherSecret() {
		JwtService foreign = new JwtServiceImpl(new AuthProperties(
				"another-access-secret-that-is-at-least-32-bytes", "test-refresh-secret-that-is-at-least-32-bytes", 3600));
		String token = foreign.generateToken(user(5L, "USER"), JwtType.ACCESS);
		redis.put(CacheType.USER_SESSION + ":" + token, user(5L, "USER"));
		sendWith(token);

		assertThatThrownBy(() -> authentication.findUserByToken(token)).isInstanceOf(AuthException.class);
	}

	@Test
	void rejectsSessionOfAnotherUser() {
		String token = jwt.generateToken(user(5L, "USER"), JwtType.ACCESS);
		redis.put(CacheType.USER_SESSION + ":" + token, user(6L, "USER"));
		sendWith(token);

		assertThatThrownBy(() -> authentication.findUserByToken(token)).isInstanceOf(AuthException.class);
	}

	@Test
	void rejectsBlockedUser() {
		String token = login(5L, SessionAuthenticationService.BLOCKED_ROLE);

		assertThatThrownBy(() -> authentication.findUserByToken(token)).isInstanceOf(UserBlockedException.class);
	}

	private String login(Long userId, String role) {
		String token = sessions.createSession(user(userId, role)).getToken();
		sendWith(token);
		return token;
	}

	private void sendWith(String token) {
		request.addHeader("Authorization", "Bearer " + token);
	}

	private static UserSession user(Long userId, String role) {
		return UserSession.builder().userId(userId).username("ann").email("a@b.c").role(role).build();
	}
}
