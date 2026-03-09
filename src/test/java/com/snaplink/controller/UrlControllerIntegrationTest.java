package com.snaplink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplink.model.dto.ShortenRequest;
import com.snaplink.model.dto.ShortenResponse;
import com.snaplink.service.CacheService;
import com.snaplink.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private CacheService cacheService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void shortenUrl_validRequest_returns201() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .url("https://www.example.com/some/long/path")
                .build();

        RateLimitService.RateLimitResult allowed = new RateLimitService.RateLimitResult(
                true, 99, System.currentTimeMillis() / 1000 + 60, 0);
        org.mockito.Mockito.when(rateLimitService.tryConsume(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(allowed);

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/some/long/path"));
    }

    @Test
    void shortenUrl_invalidUrl_returns400() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .url("not-a-url")
                .build();

        RateLimitService.RateLimitResult allowed = new RateLimitService.RateLimitResult(
                true, 99, System.currentTimeMillis() / 1000 + 60, 0);
        org.mockito.Mockito.when(rateLimitService.tryConsume(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(allowed);

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirect_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUrlDetails_nonExistentCode_returns404() throws Exception {
        RateLimitService.RateLimitResult allowed = new RateLimitService.RateLimitResult(
                true, 99, System.currentTimeMillis() / 1000 + 60, 0);
        org.mockito.Mockito.when(rateLimitService.tryConsume(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(allowed);

        mockMvc.perform(get("/api/v1/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
