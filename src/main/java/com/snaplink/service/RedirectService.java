package com.snaplink.service;

import com.snaplink.exception.UrlNotFoundException;
import com.snaplink.model.entity.Url;
import com.snaplink.repository.UrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class RedirectService {

    private final UrlRepository urlRepository;
    private final CacheService cacheService;
    private final AnalyticsService analyticsService;
    private final Counter redirectsTotal;
    private final Timer redirectLatency;

    public RedirectService(UrlRepository urlRepository,
                           CacheService cacheService,
                           AnalyticsService analyticsService,
                           MeterRegistry meterRegistry) {
        this.urlRepository = urlRepository;
        this.cacheService = cacheService;
        this.analyticsService = analyticsService;
        this.redirectsTotal = Counter.builder("snaplink.redirects.total").register(meterRegistry);
        this.redirectLatency = Timer.builder("snaplink.redirect.latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public String resolve(String shortCode, String ipAddress, String userAgent, String referrer) {
        return redirectLatency.record(() -> {
            // 1. Try cache first
            Optional<String> cached = cacheService.getCachedUrl(shortCode);
            if (cached.isPresent()) {
                recordClickAsync(shortCode, ipAddress, userAgent, referrer);
                redirectsTotal.increment();
                return cached.get();
            }

            // 2. Cache miss — query DB
            Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                    .orElseThrow(() -> new UrlNotFoundException(shortCode));

            // 3. Check expiry
            if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(OffsetDateTime.now())) {
                throw new UrlNotFoundException(shortCode);
            }

            // 4. Cache the result
            cacheService.cacheUrl(shortCode, url.getOriginalUrl());

            // 5. Record click asynchronously
            recordClickAsync(shortCode, ipAddress, userAgent, referrer);
            redirectsTotal.increment();

            return url.getOriginalUrl();
        });
    }

    private void recordClickAsync(String shortCode, String ipAddress, String userAgent, String referrer) {
        try {
            analyticsService.recordClick(shortCode, ipAddress, userAgent, referrer);
        } catch (Exception e) {
            log.warn("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }
}
