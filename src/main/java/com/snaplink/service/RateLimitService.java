package com.snaplink.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Counter rejectedCounter;

    // Lua script for atomic token bucket rate limiting
    private static final String TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            if tokens == nil then
                tokens = max_tokens
                last_refill = now
            end

            local elapsed = now - last_refill
            local new_tokens = math.min(max_tokens, tokens + (elapsed * refill_rate / 60))

            if new_tokens >= 1 then
                new_tokens = new_tokens - 1
                redis.call('HSET', key, 'tokens', new_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, ttl)
                return {1, math.floor(new_tokens), now + 60}
            else
                redis.call('HSET', key, 'tokens', new_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, ttl)
                local retry_after = math.ceil((1 - new_tokens) * 60 / refill_rate)
                return {0, 0, now + retry_after}
            end
            """;

    private final DefaultRedisScript<List> rateLimitScript;

    public RateLimitService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.rejectedCounter = Counter.builder("snaplink.ratelimit.rejected").register(meterRegistry);
        this.rateLimitScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, List.class);
    }

    public RateLimitResult tryConsume(String clientId, int maxTokensPerMinute) {
        String key = "ratelimit:" + clientId;
        long now = Instant.now().getEpochSecond();

        @SuppressWarnings("unchecked")
        List<Long> result = redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(maxTokensPerMinute),
                String.valueOf(maxTokensPerMinute), // refill_rate = max_tokens per minute
                String.valueOf(now),
                String.valueOf(120) // TTL for the key (2 minutes)
        );

        if (result == null) {
            log.warn("Rate limit script returned null for client: {}", clientId);
            return new RateLimitResult(true, maxTokensPerMinute, now + 60, 0);
        }

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetAt = result.get(2);

        if (!allowed) {
            rejectedCounter.increment();
            long retryAfter = resetAt - now;
            log.debug("Rate limit exceeded for client: {}", clientId);
            return new RateLimitResult(false, remaining, resetAt, retryAfter);
        }

        return new RateLimitResult(true, remaining, resetAt, 0);
    }

    public record RateLimitResult(
            boolean allowed,
            long remaining,
            long resetAtEpochSecond,
            long retryAfterSeconds
    ) {}
}
