package com.snaplink.service;

import com.snaplink.exception.InvalidUrlException;
import com.snaplink.exception.UrlNotFoundException;
import com.snaplink.model.dto.ShortenRequest;
import com.snaplink.model.dto.ShortenResponse;
import com.snaplink.model.entity.ApiKey;
import com.snaplink.model.entity.Url;
import com.snaplink.repository.UrlRepository;
import com.snaplink.util.Base62Encoder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final CacheService cacheService;
    private final Counter urlsCreated;
    private final String baseUrl;

    public UrlService(UrlRepository urlRepository,
                      Base62Encoder base62Encoder,
                      CacheService cacheService,
                      MeterRegistry meterRegistry,
                      @Value("${snaplink.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.base62Encoder = base62Encoder;
        this.cacheService = cacheService;
        this.baseUrl = baseUrl;
        this.urlsCreated = Counter.builder("snaplink.urls.created").register(meterRegistry);
    }

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, ApiKey apiKey) {
        validateUrl(request.getUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = validateAndUseCustomAlias(request.getCustomAlias());
        } else {
            shortCode = generateShortCode();
        }

        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .apiKeyId(apiKey != null ? apiKey.getId() : null)
                .expiresAt(request.getExpiresAt())
                .build();

        url = urlRepository.save(url);

        // If no custom alias, update with Base62 of the actual ID
        if (request.getCustomAlias() == null || request.getCustomAlias().isBlank()) {
            shortCode = base62Encoder.encode(url.getId());
            url.setShortCode(shortCode);
            url = urlRepository.save(url);
        }

        cacheService.cacheUrl(shortCode, url.getOriginalUrl());
        urlsCreated.increment();

        log.info("Created short URL: {} -> {}", shortCode, url.getOriginalUrl());

        return ShortenResponse.builder()
                .shortCode(shortCode)
                .shortUrl(baseUrl + "/" + shortCode)
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    public ShortenResponse getUrlDetails(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return ShortenResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    @Transactional
    public void deactivateUrl(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        url.setIsActive(false);
        urlRepository.save(url);
        cacheService.evictUrl(shortCode);
        log.info("Deactivated short URL: {}", shortCode);
    }

    private String generateShortCode() {
        // Temporary placeholder (max 10 chars); will be replaced after save with Base62(id)
        return "_" + Long.toString(System.nanoTime() % 100000000, 36);
    }

    private String validateAndUseCustomAlias(String alias) {
        if (!alias.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            throw new InvalidUrlException("Custom alias must be 3-20 characters and contain only letters, numbers, hyphens, or underscores");
        }
        if (urlRepository.existsByShortCode(alias)) {
            throw new InvalidUrlException("Custom alias '" + alias + "' is already taken");
        }
        return alias;
    }

    private void validateUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String host = url.getHost();
            if (host == null || host.isBlank()) {
                throw new InvalidUrlException("URL must have a valid host");
            }
            // Block localhost and private IPs
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.")
                    || host.startsWith("10.") || host.startsWith("172.16.")) {
                throw new InvalidUrlException("URLs pointing to private/local addresses are not allowed");
            }
        } catch (MalformedURLException e) {
            throw new InvalidUrlException("Invalid URL format: " + urlStr);
        }
    }
}
