package com.snaplink.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final long urlTtlHours;

    public CacheService(StringRedisTemplate redisTemplate,
                        MeterRegistry meterRegistry,
                        @Value("${snaplink.cache.url-ttl-hours:24}") long urlTtlHours) {
        this.redisTemplate = redisTemplate;
        this.urlTtlHours = urlTtlHours;
        this.cacheHits = Counter.builder("snaplink.cache.hits").register(meterRegistry);
        this.cacheMisses = Counter.builder("snaplink.cache.misses").register(meterRegistry);
    }

    public Optional<String> getCachedUrl(String shortCode) {
        String key = "url:" + shortCode;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            cacheHits.increment();
            log.debug("Cache HIT for short code: {}", shortCode);
            return Optional.of(value);
        }
        cacheMisses.increment();
        log.debug("Cache MISS for short code: {}", shortCode);
        return Optional.empty();
    }

    public void cacheUrl(String shortCode, String originalUrl) {
        String key = "url:" + shortCode;
        redisTemplate.opsForValue().set(key, originalUrl, Duration.ofHours(urlTtlHours));
        log.debug("Cached URL for short code: {}", shortCode);
    }

    public void evictUrl(String shortCode) {
        String key = "url:" + shortCode;
        redisTemplate.delete(key);
        log.debug("Evicted cache for short code: {}", shortCode);
    }
}
