package com.snaplink.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}
