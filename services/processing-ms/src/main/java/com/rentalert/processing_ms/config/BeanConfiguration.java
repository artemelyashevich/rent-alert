package com.rentalert.processing_ms.config;

import com.rentalert.common.service.LockService;
import com.rentalert.common.service.MetricService;
import com.rentalert.common.service.RestService;
import com.rentalert.common.service.impl.RedisLockServiceImpl;
import com.rentalert.common.service.impl.RestServiceImpl;
import com.rentalert.dto.GlobalConstants;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@EnableScheduling
@EnableJpaAuditing
@Configuration
@EnableConfigurationProperties(ApplicationProperties.class)
public class BeanConfiguration {

    private static final String SCHEDULE_PARSER_THREAD_PREFIX = "rent-parser-%s";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    @Bean(name = GlobalConstants.SCHEDULER_PARSER)
    public ScheduledExecutorService parserExecutorService() {
          return Executors.newScheduledThreadPool(
            6,
            r -> {
                Thread t = new Thread(r);
                t.setName(SCHEDULE_PARSER_THREAD_PREFIX.formatted(t.threadId()));
                t.setDaemon(true);
                return t;
            }
        );
    }

    @Bean
    public RestService restService() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestServiceImpl(RestClient.builder().requestFactory(requestFactory).build());
    }

    @Bean
    public LockService lockService(StringRedisTemplate stringRedisTemplate) {
        return new RedisLockServiceImpl(stringRedisTemplate);
    }

    @Bean
    public MetricService metricService(MeterRegistry meterRegistry) {
        return new MetricService(meterRegistry);
    }
}
