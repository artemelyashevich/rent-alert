package com.rentalert.common.security;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.exception.UserBlockedException;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigurationTests {

    private static final String INTERNAL_TOKEN = "internal-secret";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(TestSecurityConfiguration.class, TestWebConfiguration.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(AuthenticationService.class, () -> SecurityConfigurationTests::findUserByToken)
            .withPropertyValues("app.ip-rate-limit.enabled=false", "app.rate-limit.enabled=false",
                    "app.internal.token=" + INTERNAL_TOKEN);

    @Configuration
    static class TestSecurityConfiguration extends AbstractSecurityConfiguration {
        @Override
        protected RequestMatcher publicUrls() {
            return PathPatternRequestMatcher.withDefaults().matcher("/public/**");
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfiguration {
        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/public/ping")
        String ping() {
            return "pong";
        }

        @GetMapping("/private/me")
        String me(@AuthenticationPrincipal AuthenticatedUser user) {
            return String.valueOf(user.userId());
        }

        @GetMapping("/svc/internal/ping")
        String internalPing() {
            return "internal";
        }

        @GetMapping("/svc/internal/me")
        String internalMe(@AuthenticationPrincipal AuthenticatedUser user) {
            return String.valueOf(user.userId());
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin")
        String admin() {
            return "admin";
        }
    }

    private static AuthenticatedUser findUserByToken(String token) {
        return switch (token) {
            case "user-token" -> new AuthenticatedUser(1L, "ann", null, "USER");
            case "admin-token" -> new AuthenticatedUser(2L, "bob", null, "ADMIN");
            case "blocked-token" -> throw new UserBlockedException();
            default -> throw new AuthException("Invalid token");
        };
    }

    @Test
    void rejectsRequestWithoutToken() {
        perform("/private/me", null, result -> result.andExpect(status().isUnauthorized()));
    }

    @Test
    void rejectsInvalidToken() {
        perform("/private/me", "garbage", result -> result
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.details.errorCode").value("401")));
    }

    @Test
    void authenticatesValidTokenAndExposesPrincipal() {
        perform("/private/me", "user-token", result -> result
                .andExpect(status().isOk())
                .andExpect(content().string("1")));
    }

    @Test
    void rejectsBlockedUser() {
        perform("/private/me", "blocked-token", result -> result
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.errorCode").value("USER_BLOCKED")));
    }

    @Test
    void publicUrlOfSubclassIsOpenAndIgnoresToken() {
        perform("/public/ping", null, result -> result.andExpect(status().isOk()));
        perform("/public/ping", "garbage", result -> result
                .andExpect(status().isOk())
                .andExpect(content().string("pong")));
    }

    @Test
    void methodSecurityChecksRole() {
        perform("/admin", "user-token", result -> result.andExpect(status().isForbidden()));
        perform("/admin", "admin-token", result -> result.andExpect(status().isOk()));
    }

    @Test
    void internalUrlAcceptsServiceToken() {
        perform("/svc/internal/ping", null, INTERNAL_TOKEN, result -> result
                .andExpect(status().isOk())
                .andExpect(content().string("internal")));
    }

    @Test
    void internalUrlRejectsMissingOrWrongServiceToken() {
        perform("/svc/internal/ping", null, null, result -> result.andExpect(status().isForbidden()));
        perform("/svc/internal/ping", null, "wrong", result -> result.andExpect(status().isForbidden()));
    }

    @Test
    void internalUrlRejectsUserToken() {
        perform("/svc/internal/ping", "admin-token", null, result -> result.andExpect(status().isForbidden()));
    }

    @Test
    void internalUrlSeesUserPassedAlongWithServiceToken() {
        perform("/svc/internal/me", "user-token", INTERNAL_TOKEN, result -> result
                .andExpect(status().isOk())
                .andExpect(content().string("1")));
    }

    @Test
    void internalUrlRejectsInvalidUserTokenEvenWithServiceToken() {
        perform("/svc/internal/me", "garbage", INTERNAL_TOKEN, result -> result.andExpect(status().isUnauthorized()));
    }

    @Test
    void serviceTokenDoesNotOpenUserEndpoints() {
        perform("/private/me", null, INTERNAL_TOKEN, result -> result.andExpect(status().isUnauthorized()));
    }

    private interface ResultCheck {
        void check(ResultActions result) throws Exception;
    }

    private void perform(String url, String token, ResultCheck check) {
        perform(url, token, null, check);
    }

    private void perform(String url, String token, String internalToken, ResultCheck check) {
        runner.run(context -> {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                    .build();
            var request = get(url);
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            if (internalToken != null) {
                request.header(InternalAuth.HEADER, internalToken);
            }
            check.check(mockMvc.perform(request));
        });
    }
}
