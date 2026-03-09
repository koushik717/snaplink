package com.snaplink.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Invalid URL format")
    private String url;

    private String customAlias;

    private OffsetDateTime expiresAt;
}
