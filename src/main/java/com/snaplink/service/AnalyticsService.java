package com.snaplink.service;

import com.snaplink.exception.UrlNotFoundException;
import com.snaplink.model.dto.UrlStatsResponse;
import com.snaplink.model.entity.Click;
import com.snaplink.model.entity.Url;
import com.snaplink.repository.ClickRepository;
import com.snaplink.repository.UrlRepository;
import com.snaplink.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final UserAgentParser userAgentParser;

    @Async
    @Transactional
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referrer) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode).orElse(null);
        if (url == null) {
            log.warn("Cannot record click — URL not found for short code: {}", shortCode);
            return;
        }

        String deviceType = userAgentParser.parseDeviceType(userAgent);
        String browser = userAgentParser.parseBrowser(userAgent);

        Click click = Click.builder()
                .urlId(url.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(referrer != null ? referrer : "direct")
                .deviceType(deviceType)
                .browser(browser)
                .build();

        clickRepository.save(click);
        urlRepository.incrementClickCount(url.getId());
        log.debug("Recorded click for short code: {}", shortCode);
    }

    public UrlStatsResponse getStats(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        long totalClicks = url.getClickCount();
        long uniqueVisitors = clickRepository.countDistinctIpByUrlId(url.getId());

        List<UrlStatsResponse.CountEntry> topCountries = clickRepository.countByCountry(url.getId())
                .stream()
                .limit(10)
                .map(row -> new UrlStatsResponse.CountEntry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<UrlStatsResponse.CountEntry> topReferrers = clickRepository.countByReferrer(url.getId())
                .stream()
                .limit(10)
                .map(row -> new UrlStatsResponse.CountEntry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        Map<String, Long> clicksByDevice = clickRepository.countByDeviceType(url.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return UrlStatsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(url.getOriginalUrl())
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .createdAt(url.getCreatedAt())
                .topCountries(topCountries)
                .topReferrers(topReferrers)
                .clicksByDevice(clicksByDevice)
                .build();
    }

    public List<UrlStatsResponse.CountEntry> getReferrers(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return clickRepository.countByReferrer(url.getId())
                .stream()
                .map(row -> new UrlStatsResponse.CountEntry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<UrlStatsResponse.CountEntry> getCountries(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return clickRepository.countByCountry(url.getId())
                .stream()
                .map(row -> new UrlStatsResponse.CountEntry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getDevices(String shortCode) {
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return clickRepository.countByDeviceType(url.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
