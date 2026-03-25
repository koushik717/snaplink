# SnapLink — High-Throughput URL Shortener

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white) ![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=flat&logo=kubernetes&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)

[Live Demo](https://snaplink-1-0uyu.onrender.com/swagger-ui.html) | [API Health](https://snaplink-1-0uyu.onrender.com/actuator/health)

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
| Orchestration | Kubernetes (minikube, Helm) | Production-grade container management |
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
- **Kubernetes deployment** with HPA (1-5 replicas), ConfigMaps for env config, and zero-downtime rolling updates
- **Zero-Downtime AWS Deployment** via Terraform (ECS Fargate, RDS, ElastiCache, ALB)
- **Automated CI/CD** via GitHub Actions to Amazon ECR and ECS
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
# App: https://snaplink-1-0uyu.onrender.com
# Swagger: https://snaplink-1-0uyu.onrender.com/swagger-ui.html
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
curl https://snaplink-1-0uyu.onrender.com/actuator/health
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
curl -X POST https://snaplink-1-0uyu.onrender.com/api/v1/shorten \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"url":"https://www.example.com/very/long/path"}'
```
```json
{
  "shortCode": "aB3x7K",
  "shortUrl": "https://snaplink-1-0uyu.onrender.com/aB3x7K",
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

Visit https://snaplink-1-0uyu.onrender.com/swagger-ui.html for interactive API documentation.

## AWS Deployment (Terraform + ECS Fargate)

SnapLink is designed to be deployed on AWS using **Terraform** for Infrastructure as Code and **GitHub Actions** for CI/CD.

### Infrastructure Architecture

The Terraform configuration provisions a highly available, production-ready environment:

- **VPC**: Multi-AZ public and private subnets with a NAT Gateway.
- **Compute (ECS Fargate)**: Serverless container execution scaling securely in private subnets.
- **Database (RDS PostgreSQL)**: Managed relational database in private subnets.
- **Cache (ElastiCache Redis)**: Managed in-memory data store for caching and rate limiting.
- **Load Balancer (ALB)**: Application Load Balancer in public subnets routing external traffic to ECS.
- **Registry (ECR)**: Private Docker registry for application images.
- **Monitoring (CloudWatch)**: Alarms for high CPU/Memory and ALB latency.

### CI/CD Pipeline

The `.github/workflows/deploy-aws.yml` automates deployments:
1. Triggers on push to `main` branch.
2. Authenticates with AWS using IAM credentials.
3. Builds and tags the Docker image with the commit SHA and `latest`.
4. Pushes the image to Amazon ECR.
5. Updates the ECS Task Definition with the new image.
6. Deploys the new task to ECS Fargate with zero downtime.

### How to Deploy

1. Set up your AWS credentials as GitHub Repository Secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

2. Initialize and apply the Terraform configuration:
```bash
cd terraform
terraform init
terraform apply
```

3. Push to `main` to trigger the GitHub Actions workflow, which will build and deploy your Docker image to the ECS cluster.

---

## Kubernetes Deployment (local minikube)

The `k8s/` directory contains production-style manifests and a Helm chart for deploying SnapLink locally.

### Prerequisites

```bash
brew install minikube helm
minikube start --driver=docker --cpus=4 --memory=4096
minikube addons enable metrics-server   # required for HPA
```

### Option A — Raw manifests (step by step, best for learning)

```bash
# Build image inside minikube's Docker daemon
eval $(minikube docker-env)
docker build -t snaplink:latest .

# Deploy in dependency order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/app.yaml
kubectl apply -f k8s/hpa.yaml

# Watch everything come up
kubectl get pods -n snaplink -w
```

### Option B — Helm chart (one command, best for repeatable deploys)

```bash
eval $(minikube docker-env) && docker build -t snaplink:latest .
helm install snaplink ./snaplink-chart
```

Override values at install time:
```bash
# Deploy 3 replicas with a custom DB password
helm install snaplink ./snaplink-chart \
  --set app.replicas=3 \
  --set postgres.password=mysecretpassword
```

Upgrade a running deployment:
```bash
helm upgrade snaplink ./snaplink-chart --set app.replicas=4
```

Uninstall:
```bash
helm uninstall snaplink
```

### Access the app

```bash
# Port-forward to localhost:8090
kubectl port-forward svc/snaplink-app 8090:8080 -n snaplink

# Open in browser
open http://localhost:8090
open http://localhost:8090/swagger-ui.html
curl http://localhost:8090/actuator/health
```

### kubectl cheatsheet (run on this cluster)

```bash
# See all running pods
kubectl get pods -n snaplink

# Inspect a pod — see events, probe status, env vars, resource usage
kubectl describe pod -l app=snaplink-app -n snaplink

# Stream live logs from the app
kubectl logs -l app=snaplink-app -n snaplink -f

# Get all services (note ClusterIP vs NodePort)
kubectl get svc -n snaplink

# Scale to 3 replicas (zero-downtime rolling update)
kubectl scale deployment snaplink-app --replicas=3 -n snaplink

# Roll back to the previous image version
kubectl rollout undo deployment/snaplink-app -n snaplink

# Watch the HPA auto-scale in real time
kubectl get hpa snaplink-hpa -n snaplink -w

# Execute a command inside a running pod
kubectl exec -it deployment/snaplink-app -n snaplink -- /bin/sh
```

### Generate load to trigger HPA

```bash
# Run a load generator pod, watch HPA scale 1→5 replicas
kubectl run -i --tty load-gen --rm --image=busybox:1.28 \
  --restart=Never -n snaplink -- /bin/sh -c \
  "while sleep 0.01; do wget -q -O- http://snaplink-app:8080/actuator/health; done"

# In another terminal, watch autoscaling
kubectl get hpa snaplink-hpa -n snaplink -w
```

### K8s manifest structure

```
k8s/
├── namespace.yaml    # Isolation boundary: all objects live in "snaplink" namespace
├── configmap.yaml    # Non-sensitive config (DB URL, Redis host, profile)
├── secret.yaml       # Sensitive config (passwords) — base64 encoded
├── postgres.yaml     # Postgres Deployment + ClusterIP Service
├── redis.yaml        # Redis Deployment + ClusterIP Service
├── app.yaml          # SnapLink Deployment + NodePort Service (rolling update strategy)
└── hpa.yaml          # HorizontalPodAutoscaler: 1-5 replicas at 70% CPU

snaplink-chart/       # Helm chart — parameterises all of the above
├── Chart.yaml
├── values.yaml       # All tuneable defaults in one place
└── templates/        # Same manifests but with {{ .Values.* }} placeholders
```

### Resume bullets

```
• Orchestrated 3-service deployment (Spring Boot + Redis + PostgreSQL) on Kubernetes
  with readiness probes, ConfigMaps, Secrets, and zero-downtime rolling updates

• Implemented HorizontalPodAutoscaler scaling SnapLink from 1-5 replicas at 70% CPU
  threshold; packaged full deployment as a Helm chart for one-command installs
```
