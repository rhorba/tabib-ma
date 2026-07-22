# DevOps/DevSecOps: Tabib.ma
**References**: docs/system-design-tabib-ma.md, docs/security-tabib-ma.md, docs/test-strategy-tabib-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DevOps/DevSecOps | **Status**: Draft

## 1. YAGNI Baseline
Docker Compose for dev and single-server deployment for v1 — **no Kubernetes**. System Design's capacity estimate (~3 RPS avg, bursts to ~20 RPS) doesn't justify orchestration overhead. Re-evaluate only if horizontal scale-out (System Design SDR-1 trigger) actually happens.

## 2. Repository / Build Layout
```
tabib-ma/
├── backend/       (Spring Boot, Maven or Gradle — Tech Lead to confirm build tool preference)
├── frontend/      (React + Vite + Tailwind)
├── docker-compose.yml
├── docker-compose.dev.yml
├── .github/workflows/ci.yml
└── docs/ (this chain)
```

## 3. CI/CD Pipeline (GitHub Actions)

```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  backend-lint-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: tabibma_test
          POSTGRES_USER: tabibma
          POSTGRES_PASSWORD: test
        ports: ["5432:5432"]
        options: >-
          --health-cmd pg_isready --health-interval 5s --health-timeout 5s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Build & Test (backend)
        working-directory: backend
        run: ./mvnw -B verify -Dspring.datasource.url=jdbc:postgresql://localhost:5432/tabibma_test
      - name: Coverage report
        working-directory: backend
        run: ./mvnw jacoco:report
      - name: Enforce coverage gate (80%)
        working-directory: backend
        run: ./mvnw jacoco:check

  frontend-lint-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - working-directory: frontend
        run: npm ci
      - working-directory: frontend
        run: npm run lint
      - working-directory: frontend
        run: npm test -- --coverage

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: SAST (OWASP rules)
        uses: returntocorp/semgrep-action@v1
        with: { config: p/owasp-top-ten }
      - name: Dependency scan (backend + frontend)
        uses: aquasecurity/trivy-action@master
        with: { scan-type: fs, severity: CRITICAL,HIGH, exit-code: 1 }
      - name: Secrets scan
        uses: gitleaks/gitleaks-action@v2

  build-images:
    needs: [backend-lint-test, frontend-lint-test, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build backend image
        run: docker build -t tabibma-backend:${{ github.sha }} ./backend
      - name: Build frontend image
        run: docker build -t tabibma-frontend:${{ github.sha }} ./frontend
      - name: Scan images
        run: |
          trivy image --severity CRITICAL,HIGH --exit-code 1 tabibma-backend:${{ github.sha }}
          trivy image --severity CRITICAL,HIGH --exit-code 1 tabibma-frontend:${{ github.sha }}
```
Coverage gate is enforced **in CI itself** (`jacoco:check` fails the build below 80%), not just checked manually — matches the project's mandatory 80% rule.

## 4. Dockerfiles

### Backend (Spring Boot)
```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend (React, served via Nginx)
```dockerfile
FROM node:20-slim AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN addgroup -S app && adduser -S app -G app && \
    chown -R app:app /usr/share/nginx/html /var/cache/nginx /var/run
USER app
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/ || exit 1
EXPOSE 8080
```
Both images run as non-root (Security doc infrastructure checklist), pinned base image tags, no secrets baked in (env vars injected at runtime only).

## 5. Docker Compose (dev)
```yaml
services:
  backend:
    build: ./backend
    ports: ["8080:8080"]
    env_file: .env
    depends_on:
      db: { condition: service_healthy }
      redis: { condition: service_started }
  frontend:
    build: ./frontend
    ports: ["3000:8080"]
    env_file: .env
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: tabibma
      POSTGRES_USER: tabibma
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tabibma"]
      interval: 5s
  redis:
    image: redis:7-alpine
    volumes: ["redisdata:/data"]
volumes:
  pgdata:
  redisdata:
```

## 6. Infrastructure Topology (v1)
Matches System Design Section 3 directly:
```
[CDN] → [Load Balancer, TLS termination] → [Single Docker host: backend + frontend + Redis containers]
                                                     ↓
                                          [Managed Postgres (separate, not in the app's Docker host)]
                                                     ↓
                              [External: CMI, managed WebRTC vendor, SMTP/SMS provider]
```
- **Managed Postgres** (not self-hosted in Docker) — automated backups/PITR handled by the provider, matches DBA backup strategy without reinventing it.
- **Redis** can run as a container alongside the app at this scale (cache only, not a durability-critical store) — move to a managed instance if it ever needs to hold anything more durable than cache data.
- **Managed WebRTC vendor** (System Design SDR-3, still open): recommend evaluating **Twilio Video** or **Daily.co** early in Sprint 2 — both have pay-per-minute pricing suited to uncertain early volume and documented TURN fallback. Final selection needs a short latency test from Morocco before commit; flagged back to Tech Lead as a Sprint 2 spike, not a Sprint 1 blocker.

## 7. Monitoring Stack
```
App (Spring Boot Actuator + Micrometer) → Prometheus → Grafana dashboards
Logs (structured JSON via Logback) → Loki
Errors → Sentry (both backend and frontend)
Alerts → Slack/PagerDuty on: error rate > 1%/5min, p99 latency > 2s/5min, CPU > 80%/10min,
         disk > 85%, health check failures, cert expiry < 14 days, CMI webhook failure rate spike
         (product-specific alert — payment webhook failures are a direct revenue/trust risk)
```

## 8. Security Scanning (ties to Security doc Section 8 checklist)
```bash
semgrep ci --config p/owasp-top-ten --config p/security-audit         # SAST
trivy fs --severity CRITICAL,HIGH .                                    # dependency (SCA)
trivy image --severity CRITICAL,HIGH tabibma-backend:latest            # container image
trivy image --severity CRITICAL,HIGH tabibma-frontend:latest
gitleaks detect --source . --verbose                                   # secrets scan
```
All four run in CI on every push (Section 3) — this is what makes the Security doc's checklist enforceable rather than aspirational.

## 9. Secrets Management
Real secrets (CMI keys, JWT signing key, SMTP/SMS credentials, DB password) are injected via the hosting provider's secrets manager or GitHub Actions encrypted secrets for CI — **never** committed, and `.env.example` (already in repo root) contains placeholders only, per Security doc Section 6.

## 10. CI Monitoring Protocol (acknowledged, per project rule 11)
```
After every push:
  1. gh run watch — do not proceed while CI is running
  2. GREEN → log "CI: green on <branch> (<commit>)" to .logs/activity.md
  3. RED → diagnose (test/lint/build/security scan failure), fix, push, repeat from step 1
  4. No task is "done" and no SHIP phase begins while CI is red
```
This applies starting Sprint 2 once there is code to push — Sprint 1 docs commit still gets this treatment for the initial `docs/` push (no code to fail lint/tests against, so CI here is effectively just secrets-scan + basic repo hygiene checks).

## 11. Handoff Points
- **→ Deployment**: Docker images, compose files, infra topology above — ready for environment provisioning.
- **→ Tech Lead**: Managed WebRTC vendor decision needs a Sprint 2 spike (Section 6) before Consultation module implementation starts.
- **→ Security Engineer**: Scanning results feed back for triage; secrets management approach (Section 9) implements Security doc Section 6 requirement.
- **→ PM**: Infra is ready to support Sprint 2 implementation start once this doc is approved.
