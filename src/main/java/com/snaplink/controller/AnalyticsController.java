package com.snaplink.controller;

import com.snaplink.model.dto.UrlStatsResponse;
import com.snaplink.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "URL click analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get overall stats for a short URL")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getStats(shortCode));
    }

    @GetMapping("/{shortCode}/referrers")
    @Operation(summary = "Get top referrers for a short URL")
    public ResponseEntity<List<UrlStatsResponse.CountEntry>> getReferrers(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getReferrers(shortCode));
    }

    @GetMapping("/{shortCode}/countries")
    @Operation(summary = "Get clicks by country for a short URL")
    public ResponseEntity<List<UrlStatsResponse.CountEntry>> getCountries(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getCountries(shortCode));
    }

    @GetMapping("/{shortCode}/devices")
    @Operation(summary = "Get clicks by device type for a short URL")
    public ResponseEntity<Map<String, Long>> getDevices(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getDevices(shortCode));
    }
}
