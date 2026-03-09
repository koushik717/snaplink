package com.snaplink.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlStatsResponse {
    private String shortCode;
    private String originalUrl;
    private Long totalClicks;
    private Long uniqueVisitors;
    private OffsetDateTime createdAt;
    private List<CountEntry> topCountries;
    private List<CountEntry> topReferrers;
    private Map<String, Long> clicksByDevice;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountEntry {
        private String name;
        private Long clicks;
    }
}
