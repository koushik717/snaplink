package com.snaplink.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate, new SimpleMeterRegistry());
    }

    @Test
    void tryConsume_allowed_returnsAllowed() {
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(Arrays.asList(1L, 99L, System.currentTimeMillis() / 1000 + 60));

        RateLimitService.RateLimitResult result = rateLimitService.tryConsume("test-client", 100);

        assertTrue(result.allowed());
    }

    @Test
    void tryConsume_rejected_returnsNotAllowed() {
        long now = System.currentTimeMillis() / 1000;
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(Arrays.asList(0L, 0L, now + 30));

        RateLimitService.RateLimitResult result = rateLimitService.tryConsume("test-client", 100);

        assertFalse(result.allowed());
    }

    @Test
    void tryConsume_nullResult_fallsBackToAllowed() {
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(null);

        RateLimitService.RateLimitResult result = rateLimitService.tryConsume("test-client", 100);

        assertTrue(result.allowed());
    }
}
