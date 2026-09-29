package com.rentalert.common.security;

import com.rentalert.common.common.I18nService;
import com.rentalert.common.config.InternalAuthProperties;
import com.rentalert.common.config.IpRateLimitProperties;
import com.rentalert.common.config.RequestRateLimitProperties;
import com.rentalert.common.util.JsonHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties
public abstract class AbstractSecurityConfiguration {

    protected RequestMatcher publicUrls() {
        return request -> false;
    }

    protected RequestMatcher internalUrls() {
        return PathPatternRequestMatcher.withDefaults().matcher(InternalAuth.URL_PATTERN);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(PublicUrls.PUBLIC_URLS);
    }

    // Bound to app.ip-rate-limit.* / app.rate-limit.* by the @ConfigurationProperties on the classes
    @Bean
    public IpRateLimitProperties ipRateLimitProperties() {
        return new IpRateLimitProperties();
    }

    @Bean
    public RequestRateLimitProperties requestRateLimitProperties() {
        return new RequestRateLimitProperties();
    }

    @Bean
    public InternalAuthProperties internalAuthProperties() {
        return new InternalAuthProperties();
    }

    @Bean
    public IpAccessGuard ipAccessGuard(StringRedisTemplate redisTemplate, IpRateLimitProperties ipRateLimitProperties, RequestRateLimitProperties requestRateLimitProperties) {
        return new IpAccessGuard(redisTemplate, ipRateLimitProperties, requestRateLimitProperties);
    }

    @Bean
    public TokenAuthenticationProvider tokenAuthenticationProvider(AuthenticationService authenticationService) {
        return new TokenAuthenticationProvider(authenticationService);
    }

    @Bean
    public AuthenticationManager authenticationManager(TokenAuthenticationProvider tokenAuthenticationProvider) {
        return new ProviderManager(tokenAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager, IpAccessGuard ipAccessGuard, InternalAuthProperties internalAuthProperties, MessageSource messageSource, HttpServletRequest request) {
        RequestMatcher publicUrls = publicUrls();
        RequestMatcher internalUrls = internalUrls();
        I18nService i18nService = new I18nService(messageSource, request);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(ipAccessGuard, JsonHelper.mapper, i18nService);
        InternalTokenFilter internalTokenFilter = new InternalTokenFilter(internalUrls, internalAuthProperties);
        JwtTokenSecurityFilter jwtTokenSecurityFilter = new JwtTokenSecurityFilter(publicUrls, ipAccessGuard, i18nService, JsonHelper.mapper, authenticationManager);
        http
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .exceptionHandling(exc -> exc
                .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> {
                    log.error("access denied");
                    res.setStatus(FORBIDDEN.value());
                }))
            .addFilterBefore(internalTokenFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(jwtTokenSecurityFilter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(req -> req
                .requestMatchers(internalUrls).hasAuthority(InternalAuth.AUTHORITY)
                .requestMatchers(publicUrls).permitAll()
                .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
