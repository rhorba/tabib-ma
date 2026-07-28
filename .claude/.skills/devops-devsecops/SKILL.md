---
name: devops-devsecops
description: >
  Combined DevOps and DevSecOps skill for CI/CD pipelines, infrastructure as code, security scanning,
  container hardening, cloud infrastructure, and monitoring. Use when the user needs CI/CD setup,
  Docker/Kubernetes configs, Terraform/IaC, security scanning (SAST/DAST/SCA), secrets management,
  monitoring/alerting, cloud infra, or any ops + security work. Trigger on: "CI/CD", "pipeline",
  "Docker", "Kubernetes", "Terraform", "infrastructure", "monitoring", "alerting", "Grafana",
  "Prometheus", "security scan", "Trivy", "Semgrep", "Checkov", "Snyk", "hardening", "DevOps",
  "DevSecOps", "infra as code", "cloud", "AWS", "GCP", "Azure", or ops/security work.
---

# DevOps & DevSecOps Engineer

## Role
You build and secure the infrastructure, pipelines, and operational foundation.

**YAGNI for DevOps**: Docker Compose before Kubernetes. Single server before auto-scaling. Simple CI before 15-stage pipelines. Add complexity only when current setup provably can't handle the load.

## Decision Router
Read the relevant reference file ONLY when doing a deep dive:

| Task | Reference |
|---|---|
| Security scanning, OWASP, code vulns | `references/code-security.md` |
| CI/CD pipeline setup and hardening | `references/cicd-security.md` |
| Docker, K8s, container hardening | `references/container-k8s-security.md` |
| AWS/GCP/Azure, IAM, cloud infra | `references/cloud-security.md` |
| Threat modeling, compliance | `references/threat-model-compliance.md` |

## Quick CI/CD Pipeline (GitHub Actions)
```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  lint-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup & Install
        run: |
          # adapt to project stack
          npm ci  # or pip install, go mod download
      - name: Lint
        run: npm run lint
      - name: Test
        run: npm test -- --coverage
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: returntocorp/semgrep-action@v1
        with:
          config: p/owasp-top-ten
      - uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs
          severity: CRITICAL,HIGH
          exit-code: 1
  build:
    needs: [lint-test, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build
        run: npm run build  # adapt to stack
```

## Dockerfile Template (secure)
```dockerfile
FROM node:20-slim@sha256:<digest> AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --production
COPY . .
RUN npm run build

FROM node:20-slim@sha256:<digest>
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=builder --chown=app:app /app/dist ./dist
COPY --from=builder --chown=app:app /app/node_modules ./node_modules
USER app
HEALTHCHECK --interval=30s CMD curl -f http://localhost:3000/health || exit 1
EXPOSE 3000
CMD ["node", "dist/index.js"]
```

## Docker Compose (dev)
```yaml
services:
  app:
    build: .
    ports: ["3000:3000"]
    env_file: .env
    depends_on:
      db: { condition: service_healthy }
    volumes: ["./src:/app/src"]  # dev hot-reload only
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app"]
      interval: 5s
volumes:
  pgdata:
```

## Infrastructure as Code (Terraform starter)
```hcl
terraform {
  required_version = ">= 1.5"
  backend "s3" {}  # remote state
}

# Adapt provider to user's cloud
provider "aws" {
  region = var.region
}
```

## Monitoring Stack
```
App → Metrics (Prometheus) → Dashboards (Grafana)
  └→ Logs (Loki/ELK) → Alerts (PagerDuty/Slack)
  └→ Traces (Jaeger/Tempo)
  └→ Errors (Sentry)
```

**Essential alerts:**
- Error rate > 1% for 5 min
- Latency p99 > 2s for 5 min
- CPU > 80% for 10 min
- Disk > 85%
- Health check failures
- Certificate expiry < 14 days

## Security Scanning Quick Commands
```bash
# SAST
semgrep ci --config p/owasp-top-ten --config p/security-audit

# SCA (dependency vulns)
trivy fs --severity CRITICAL,HIGH .

# Container image scan
trivy image --severity CRITICAL,HIGH myapp:latest

# IaC scan
checkov -d ./terraform --framework terraform

# Secrets scan
gitleaks detect --source . --verbose
```

## CI Monitoring Protocol (mandatory rule)

After **every push** to any branch, DevOps/DevSecOps must:

```
1. Watch the CI pipeline run to completion — do not move on while it is running
2. If CI is GREEN → log to .logs/activity.md: "[DATE] CI: green on <branch> (<commit>)"
3. If CI is RED →
     a. Read the failure output immediately
     b. Diagnose root cause (test failure / lint / build / security scan)
     c. Fix the issue in the working branch
     d. Push fix and repeat from step 1
     e. Log each iteration to .logs/issues.md
4. A sprint task is NEVER marked "done" while CI is red on its branch
5. Never move to SHIP phase with a red CI — this is a hard gate
```

**Monitoring commands:**
```bash
# GitHub CLI — watch live CI status
gh run watch

# Poll until green
gh run watch --exit-status

# View failed run details
gh run view --log-failed

# Re-run failed jobs only
gh run rerun --failed
```

**Common CI failure → fix mapping:**
| Failure | Likely Cause | Fix |
|---|---|---|
| Test failure | Logic bug or broken fixture | Fix the code, not the test |
| Lint error | Style violation | Run formatter and push |
| Build error | Missing dependency / type error | Resolve locally first |
| Security scan | New CVE in dependency | Update package, re-scan |
| Coverage < 80% | Untested path | Write the missing test |
| Permission error | Missing secret or env var | Add to repo secrets |

## Handoff Points
- **← From Tech Lead**: Receives infra requirements
- **← From Security Engineer**: Receives scanning policies, compliance rules to enforce
- **← From Deployment**: Receives deployment configs to secure
- **← From DBA**: Receives backup/replication infra requirements
- **→ Security Engineer**: Reports scan findings for triage and risk assessment
- **→ Tester**: Provides staging environments
- **→ Deployment**: Provides hardened pipeline and infra — only when CI is GREEN
- **→ DBA**: Provides database infra (instances, networking, encryption)
- **→ PM**: Reports security posture, infrastructure status, CI health
