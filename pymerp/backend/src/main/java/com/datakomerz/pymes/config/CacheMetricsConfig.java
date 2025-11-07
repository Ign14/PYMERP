package com.datakomerz.pymes.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Collection;
import java.util.Collections;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheMetricsConfig {

  @Bean("redisCacheMetricsRegistrar")
  public CacheMetricsRegistrar cacheMetricsRegistrar(MeterRegistry registry,
                                                    Collection<CacheManager> cacheManagers,
                                                    ObjectProvider<StringRedisTemplate> templateProvider) {
    CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(registry, Collections.emptyList());
    cacheManagers.forEach(cacheManager -> cacheManager.getCacheNames().forEach(cacheName -> {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache instanceof RedisCache redisCache) {
        registrar.bindCacheToRegistry(cache);
        StringRedisTemplate template = templateProvider.getIfAvailable();
        if (template != null) {
          final String gaugeCacheName = cacheName;
          registry.gauge(
            "cache.size",
            Tags.of("cache", gaugeCacheName),
            template,
            redisTemplate -> estimateSize(redisTemplate, gaugeCacheName)
          );
        }
      }
    }));
    return registrar;
  }

  private double estimateSize(StringRedisTemplate template, String cacheName) {
    Long count = template.execute((RedisCallback<Long>) connection -> {
      long total = 0L;
      ScanOptions options = ScanOptions.scanOptions()
        .match(cacheName + "::" + "*")
        .count(500)
        .build();
      try (Cursor<byte[]> cursor = connection.scan(options)) {
        while (cursor != null && cursor.hasNext()) {
          cursor.next();
          total++;
        }
      }
      return total;
    });
    return count == null ? 0D : count.doubleValue();
  }
}
