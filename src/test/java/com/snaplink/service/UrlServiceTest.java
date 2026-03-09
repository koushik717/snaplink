package com.snaplink.service;

import com.snaplink.exception.InvalidUrlException;
import com.snaplink.model.dto.ShortenRequest;
import com.snaplink.model.dto.ShortenResponse;
import com.snaplink.model.entity.Url;
import com.snaplink.repository.UrlRepository;
import com.snaplink.util.Base62Encoder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private CacheService cacheService;

    private Base62Encoder base62Encoder;
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();
        urlService = new UrlService(
                urlRepository,
                base62Encoder,
                cacheService,
                new SimpleMeterRegistry(),
                "http://localhost:8080"
        );
    }

    @Test
    void shortenUrl_validUrl_returnsResponse() {
        ShortenRequest request = ShortenRequest.builder()
                .url("https://www.example.com/long/path")
                .build();

        Url savedUrl = Url.builder().id(1L).shortCode("temp").originalUrl("https://www.example.com/long/path").build();
        Url updatedUrl = Url.builder().id(1L).shortCode("1").originalUrl("https://www.example.com/long/path").build();

        when(urlRepository.save(any(Url.class)))
                .thenReturn(savedUrl)
                .thenReturn(updatedUrl);

        ShortenResponse response = urlService.shortenUrl(request, null);

        assertNotNull(response);
        assertEquals("https://www.example.com/long/path", response.getOriginalUrl());
        assertNotNull(response.getShortCode());
        verify(cacheService).cacheUrl(anyString(), eq("https://www.example.com/long/path"));
    }

    @Test
    void shortenUrl_customAlias_usesAlias() {
        ShortenRequest request = ShortenRequest.builder()
                .url("https://www.example.com/path")
                .customAlias("my-link")
                .build();

        when(urlRepository.existsByShortCode("my-link")).thenReturn(false);

        Url savedUrl = Url.builder().id(1L).shortCode("my-link").originalUrl("https://www.example.com/path").build();
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        ShortenResponse response = urlService.shortenUrl(request, null);

        assertEquals("my-link", response.getShortCode());
    }

    @Test
    void shortenUrl_duplicateAlias_throwsException() {
        ShortenRequest request = ShortenRequest.builder()
                .url("https://www.example.com/path")
                .customAlias("taken")
                .build();

        when(urlRepository.existsByShortCode("taken")).thenReturn(true);

        assertThrows(InvalidUrlException.class, () -> urlService.shortenUrl(request, null));
    }

    @Test
    void shortenUrl_invalidUrl_throwsException() {
        ShortenRequest request = ShortenRequest.builder()
                .url("not-a-valid-url")
                .build();

        assertThrows(InvalidUrlException.class, () -> urlService.shortenUrl(request, null));
    }

    @Test
    void shortenUrl_localhostUrl_throwsException() {
        ShortenRequest request = ShortenRequest.builder()
                .url("http://localhost:3000/admin")
                .build();

        assertThrows(InvalidUrlException.class, () -> urlService.shortenUrl(request, null));
    }

    @Test
    void getUrlDetails_existingUrl_returnsDetails() {
        Url url = Url.builder()
                .id(1L)
                .shortCode("aB3x7K")
                .originalUrl("https://www.example.com")
                .build();

        when(urlRepository.findByShortCodeAndIsActiveTrue("aB3x7K")).thenReturn(Optional.of(url));

        ShortenResponse response = urlService.getUrlDetails("aB3x7K");

        assertEquals("aB3x7K", response.getShortCode());
        assertEquals("https://www.example.com", response.getOriginalUrl());
    }

    @Test
    void deactivateUrl_existingUrl_deactivates() {
        Url url = Url.builder().id(1L).shortCode("abc").isActive(true).build();
        when(urlRepository.findByShortCodeAndIsActiveTrue("abc")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        urlService.deactivateUrl("abc");

        assertFalse(url.getIsActive());
        verify(cacheService).evictUrl("abc");
    }
}
