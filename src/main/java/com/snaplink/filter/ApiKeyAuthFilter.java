package com.snaplink.filter;

import com.snaplink.model.entity.ApiKey;
import com.snaplink.repository.ApiKeyRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip auth for non-API paths (redirects, actuator, swagger)
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            String keyHash = hashKey(apiKey);
            Optional<ApiKey> key = apiKeyRepository.findByKeyHashAndIsActiveTrue(keyHash);

            if (key.isPresent()) {
                httpRequest.setAttribute("apiKey", key.get());
                httpRequest.setAttribute("rateLimit", key.get().getRateLimit());
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid API key\"}");
                return;
            }
        } else {
            // Unauthenticated access gets lower rate limit
            httpRequest.setAttribute("rateLimit", 20);
        }

        chain.doFilter(request, response);
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
