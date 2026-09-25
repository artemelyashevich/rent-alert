package com.rentalert.redis;

import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.RedisCacheServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@AutoConfiguration(after = DataRedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisUtilAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "jsonRedisTemplate")
	public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer valueSerializer = jsonRedisSerializer();
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(RedisSerializer.string());
		template.setHashKeySerializer(RedisSerializer.string());
		template.setValueSerializer(valueSerializer);
		template.setHashValueSerializer(valueSerializer);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean
	public CacheService cacheService(StringRedisTemplate stringRedisTemplate) {
		return new RedisCacheServiceImpl(stringRedisTemplate);
	}

	static GenericJacksonJsonRedisSerializer jsonRedisSerializer() {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType("com.rentalert.")
				.allowIfSubType("java.lang.")
				.allowIfSubType("java.util.")
				.allowIfSubType("java.time.")
				.build();
		return GenericJacksonJsonRedisSerializer.builder()
				.enableDefaultTyping(typeValidator)
				.build();
	}

}
