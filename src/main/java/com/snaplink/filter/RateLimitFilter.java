package com.snaplink.filter;

import com.snaplink.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(3)
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Only rate limit API endpoints
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Determine the client identifier for rate limiting
        String clientId = resolveClientId(httpRequest);
        Integer rateLimit = (Integer) httpRequest.getAttribute("rateLimit");
        if (rateLimit == null) {
            rateLimit = 20; // default for unauthenticated
        }

        RateLimitService.RateLimitResult result = rateLimitService.tryConsume(clientId, rateLimit);

        // Always set rate limit headers
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAtEpochSecond()));

        if (!result.allowed()) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            httpResponse.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in "
                            + result.retryAfterSeconds() + " seconds.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientId(HttpServletRequest request) {
        // Use API key if available, otherwise fall back to IP
        Object apiKey = request.getAttribute("apiKey");
        if (apiKey != null) {
            return "apikey:" + ((com.snaplink.model.entity.ApiKey) apiKey).getKeyHash();
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return "ip:" + ip.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
