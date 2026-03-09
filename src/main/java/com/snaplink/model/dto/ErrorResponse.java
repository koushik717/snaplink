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
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String requestId;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();
}
