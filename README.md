# SnapLink — High-Throughput URL Shortener

A production-grade URL shortening service designed for high throughput and observability. Features distributed Base62 ID generation, Redis caching with intelligent TTL, token-bucket rate limiting per API key, a click analytics pipeline (geo, referrer, device), full request tracing, and Prometheus/Grafana monitoring — load-tested at **12,000+ requests/second**.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                     CLIENTS                          │
│           (Browser / curl / Postman)                 │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                 SPRING BOOT APP                      │
│                                                      │
│  ┌──────────────┐  ┌──────────┐  ┌───────────────┐ │
│  │ URL Controller│  │ Redirect │  │  Analytics    │ │
│  │ POST /shorten │  │ GET /{c} │  │  GET /stats   │ │
│  └──────┬───────┘  └────┬─────┘  └──────┬────────┘ │
│         │               │               │           │
│  ┌──────▼───────────────▼───────────────▼────────┐  │
│  │              SERVICE LAYER                     │  │
│  │  URL Service · Redirect Service · Analytics    │  │
│  │  Cache Service · Rate Limit Service            │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │           CROSS-CUTTING CONCERNS               │  │
│  │  Rate Limiter · Request Tracing · Error Handler│  │
│  └────────────────────────────────────────────────┘  │
└──────┬──────────────────┬────────────────────────────┘
       │                  │
  ┌────▼────┐       ┌─────▼─────┐
  │PostgreSQL│       │   Redis   │
  │ urls     │       │ URL cache │
  │ clicks   │       │ rate      │
  │ api_keys │       │ limits    │
  └──────────┘       └───────────┘
```

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Framework | Spring Boot 3.5 (Java 17) | Most requested in backend JDs |
| Database | PostgreSQL 17 | Industry standard relational DB |
| Cache | Redis 7 | Caching + rate limiting in one |
| Monitoring | Prometheus + Grafana | Industry standard observability |
| Containerization | Docker + Docker Compose | Reproducible deployment |
| Load Testing | k6 (Grafana Labs) | Modern, scriptable load testing |
| API Docs | SpringDoc OpenAPI (Swagger) | Professional API documentation |

## Key Features

- **Base62 short codes** from sequential IDs (56B+ unique URLs)
- **Redis caching** with cache-aside pattern (< 5ms p50 redirect latency)
- **Token bucket rate limiting** per API key via Redis Lua script
- **Click analytics** pipeline (referrer, device, browser tracking)
- **Request tracing** with X-Request-Id correlation
- **Full observability** via Prometheus metrics + Grafana dashboards
- **Async click recording** — redirects are never blocked by analytics writes
- **Load tested** at 12,000+ req/sec with k6

## Performance

| Metric | Value |
|--------|-------|
| Redirect latency (cached, p50) | 4.36ms |
| Redirect latency (cached, p95) | 21.09ms |
| Throughput (sustained) | 12,432 req/sec |
| Cache hit ratio | 100% |
| Error rate under load | 0.00% |
| Total requests (70s test) | 872,067 |

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 16+
- Redis 7+

### Run with Docker Compose
```bash
docker-compose up -d
# App: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

### Run Locally
```bash
# Start PostgreSQL and Redis
# Create database: CREATE DATABASE snaplink;

# Run the app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Health check
curl http://localhost:8080/actuator/health
```

## API Reference

### Core Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/shorten` | Create a short URL |
| GET | `/{shortCode}` | Redirect to original URL (302) |
| GET | `/api/v1/urls/{shortCode}` | Get URL details |
| DELETE | `/api/v1/urls/{shortCode}` | Deactivate a short URL |

### Analytics Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/stats/{shortCode}` | Overall stats |
| GET | `/api/v1/stats/{shortCode}/referrers` | Top referrers |
| GET | `/api/v1/stats/{shortCode}/countries` | Clicks by country |
| GET | `/api/v1/stats/{shortCode}/devices` | Clicks by device type |

### Example: Create Short URL
```bash
curl -X POST http://localhost:8080/api/v1/shorten \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"url":"https://www.example.com/very/long/path"}'
```
```json
{
  "shortCode": "aB3x7K",
  "shortUrl": "http://localhost:8080/aB3x7K",
  "originalUrl": "https://www.example.com/very/long/path",
  "createdAt": "2026-03-08T10:30:00Z"
}
```

## Design Decisions

| Decision | Chosen | Why | Alternative |
|----------|--------|-----|-------------|
| ID Generation | Sequential DB ID → Base62 | Zero collisions, deterministic | Random/UUID: collision risk |
| Cache Strategy | Cache-aside (lazy loading) | Only caches what's accessed | Write-through: wastes memory |
| Rate Limiting | Token bucket (Redis Lua) | Allows bursts, industry standard | Fixed window: penalizes at boundaries |
| Click Recording | Async (@Async) | Redirects stay fast | Sync: blocks redirect |
| HTTP Redirect | 302 (temporary) | Allows analytics on every click | 301: browsers cache, kills analytics |
| API Key Storage | SHA-256 hash | Never store raw secrets | Plain text: security disaster |
| DB Migrations | Flyway | Version-controlled, repeatable | Manual SQL: error-prone |

## Running Load Tests

```bash
brew install k6
k6 run k6/load-test.js
```

## Swagger UI

Visit http://localhost:8080/swagger-ui.html for interactive API documentation.
