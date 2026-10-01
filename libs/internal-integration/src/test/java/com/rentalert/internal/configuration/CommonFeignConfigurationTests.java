package com.rentalert.internal.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.internal.client.service.impl.IdentityMsServiceImpl;
import com.rentalert.internal.client.service.impl.ListingMsServiceImpl;
import com.rentalert.internal.decoder.InternalErrorDecoder;
import com.rentalert.internal.interceptor.HeaderPropagationInterceptor;
import feign.Logger;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CommonFeignConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CommonFeignConfiguration.class,
            RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class));

    @Test
    void isRegisteredAsAutoConfiguration() {
        var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

        assertThat(candidates).contains(CommonFeignConfiguration.class.getName());
    }

    @Test
    void registersFeignBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(HeaderPropagationInterceptor.class);
            assertThat(context).getBean(IdentityMsService.class).isInstanceOf(IdentityMsServiceImpl.class);
            assertThat(context).getBean(ListingMsService.class).isInstanceOf(ListingMsServiceImpl.class);
            assertThat(context).getBean(ErrorDecoder.class).isInstanceOf(InternalErrorDecoder.class);
            assertThat(context.getBean(Logger.Level.class)).isEqualTo(Logger.Level.BASIC);
        });
    }

    @Test
    void fullLoggingEnabledByProperty() {
        runner.withPropertyValues("client.server.log-mode=true")
            .run(context -> assertThat(context.getBean(Logger.Level.class)).isEqualTo(Logger.Level.FULL));
    }

    @Test
    void appliesDefaultResilienceConfigToAnyInstance() {
        runner.run(context -> {
            RetryConfig retry = context.getBean(RetryRegistry.class).retry("identity-ms").getRetryConfig();
            assertThat(retry.getMaxAttempts()).isEqualTo(3);
            assertThat(retry.getExceptionPredicate().test(new UpstreamHttpException("x", 503, null))).isTrue();
            assertThat(retry.getExceptionPredicate().test(new UpstreamHttpException("x", 404, null))).isFalse();

            CircuitBreakerConfig circuitBreaker = context.getBean(CircuitBreakerRegistry.class)
                .circuitBreaker("identity-ms").getCircuitBreakerConfig();
            assertThat(circuitBreaker.getSlidingWindowSize()).isEqualTo(20);
            assertThat(circuitBreaker.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(10).toMillis());
            assertThat(circuitBreaker.getRecordExceptionPredicate().test(new UpstreamHttpException("x", 404, null))).isFalse();
        });
    }

    @Test
    void serviceConfigOverridesDefaults() {
        runner.withPropertyValues("resilience4j.retry.instances.identity-ms.max-attempts=5")
            .run(context -> assertThat(context.getBean(RetryRegistry.class).retry("identity-ms")
                .getRetryConfig().getMaxAttempts()).isEqualTo(5));
    }
}
