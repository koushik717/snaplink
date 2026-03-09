package com.snaplink.controller;

import com.snaplink.model.dto.ShortenRequest;
import com.snaplink.model.dto.ShortenResponse;
import com.snaplink.model.entity.ApiKey;
import com.snaplink.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "URLs", description = "URL shortening operations")
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    @Operation(summary = "Create a short URL")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {
        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("apiKey");
        ShortenResponse response = urlService.shortenUrl(request, apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/urls/{shortCode}")
    @Operation(summary = "Get URL details without redirecting")
    public ResponseEntity<ShortenResponse> getUrlDetails(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getUrlDetails(shortCode));
    }

    @DeleteMapping("/urls/{shortCode}")
    @Operation(summary = "Deactivate a short URL (soft delete)")
    public ResponseEntity<Void> deactivateUrl(@PathVariable String shortCode) {
        urlService.deactivateUrl(shortCode);
        return ResponseEntity.noContent().build();
    }
}
