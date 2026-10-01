package com.rentalert.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.rentalert.redis.service.CacheService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;

class RedisUtilAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class, RedisUtilAutoConfiguration.class));

    @Test
    void isRegisteredAsAutoConfiguration() {
        var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

        assertThat(candidates).contains(RedisUtilAutoConfiguration.class.getName());
    }

    @Test
    void registersJsonRedisTemplate() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("jsonRedisTemplate");
            RedisTemplate<?, ?> template = context.getBean("jsonRedisTemplate", RedisTemplate.class);
            assertThat(template.getValueSerializer()).isNotNull();
            // Boot's own templates are still there
            assertThat(context).hasBean("redisTemplate").hasBean("stringRedisTemplate");
            assertThat(context).hasSingleBean(CacheService.class);
        });
    }

    @Test
    void backsOffWhenUserDefinesJsonRedisTemplate() {
        contextRunner.withUserConfiguration(CustomTemplateConfig.class).run(context ->
            assertThat(context.getBean("jsonRedisTemplate")).isSameAs(context.getBean(CustomTemplateConfig.class).template));
    }

    @Test
    void backsOffWithoutConnectionFactory() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisUtilAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean("jsonRedisTemplate"));
    }

    @Test
    void serializerRoundTripsOriginalTypes() {
        var serializer = RedisUtilAutoConfiguration.jsonRedisSerializer();
        var value = new Sample("flat", 3, LocalDate.of(2026, 9, 25), List.of("a", "b"));

        assertThat(serializer.deserialize(serializer.serialize(value))).isEqualTo(value);
    }

    @Test
    void serializerRejectsTypesOutsideAllowList() {
        var serializer = RedisUtilAutoConfiguration.jsonRedisSerializer();
        byte[] json = "{\"@class\":\"javax.naming.InitialContext\"}".getBytes();

        assertThatThrownBy(() -> serializer.deserialize(json)).isInstanceOf(SerializationException.class);
    }

    record Sample(String name, int rooms, LocalDate date, List<String> tags) {

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTemplateConfig {

        final RedisTemplate<String, Object> template = new RedisTemplate<>();

        @Bean
        RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory connectionFactory) {
            template.setConnectionFactory(connectionFactory);
            return template;
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

    }

}
