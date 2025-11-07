package com.datakomerz.pymes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Central cache configuration for Redis-backed caches.
 *
 * <p>Defines dedicated TTLs per domain cache and enforces String/JSON serializers to guarantee
 * compatibility across services. Null values are never cached to avoid leaking 404 lookups.</p>
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper mapper = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .addModule(new Hibernate5JakartaModule())
      .addModule(new RedisPageModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL)
      .build();

    GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);

    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(10))
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
      .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
      "products", defaults.entryTtl(Duration.ofMinutes(10)),
      "customers", defaults.entryTtl(Duration.ofMinutes(5)),
      "suppliers", defaults.entryTtl(Duration.ofMinutes(15)),
      "companySettings", defaults.entryTtl(Duration.ofHours(1))
    );

    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(defaults)
      .withInitialCacheConfigurations(cacheConfigs)
      .build();
  }
}
