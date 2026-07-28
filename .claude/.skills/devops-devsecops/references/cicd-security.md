# CI/CD Security Reference

## Pipeline Security Architecture

### Shift-Left Scanning Stages
```
Developer Workstation → Pre-commit → CI Build → CI Test → Staging → Production → Runtime
   ├─ IDE plugins         ├─ Secrets    ├─ SAST    ├─ DAST    ├─ Smoke    ├─ WAF
   ├─ Linters             ├─ Linting    ├─ SCA     ├─ IAST    ├─ Pen test ├─ RASP
   └─ Local scans         └─ Format     ├─ Image   └─ Fuzzing └─ Config   ├─ Monitoring
                                        └─ IaC                  validation └─ Incident response
```

### Pipeline Hardening Checklist
- [ ] All pipeline configs in version control
- [ ] Branch protection on main/release branches (require reviews, status checks)
- [ ] Signed commits required for release branches
- [ ] Pipeline secrets in vault (never in YAML/env files)
- [ ] Least-privilege service accounts for CI runners
- [ ] Ephemeral build environments (no persistent state)
- [ ] Artifact signing and verification
- [ ] Immutable build artifacts (content-addressable storage)
- [ ] Audit logging on all pipeline modifications
- [ ] Network segmentation for CI runners

---

## GitHub Actions Security

### Hardened Workflow Template
```yaml
name: secure-pipeline
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

# Restrict default permissions
permissions:
  contents: read

jobs:
  secrets-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for secret scanning
      - uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  sast:
    runs-on: ubuntu-latest
    permissions:
      security-events: write  # Only what's needed
    steps:
      - uses: actions/checkout@v4
      - uses: returntocorp/semgrep-action@v1
        with:
          config: >-
            p/owasp-top-ten
            p/security-audit
            p/secrets
          generateSarif: "1"

  sca:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs
          severity: CRITICAL,HIGH
          exit-code: 1
          format: sarif
          output: trivy-results.sarif

  container-scan:
    runs-on: ubuntu-latest
    needs: [sast, sca]
    steps:
      - uses: actions/checkout@v4
      - name: Build image
        run: docker build -t app:${{ github.sha }} .
      - name: Scan image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: app:${{ github.sha }}
          severity: CRITICAL,HIGH
          exit-code: 1

  iac-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Checkov IaC scan
        uses: bridgecrewio/checkov-action@v12
        with:
          directory: ./terraform
          framework: terraform
          soft_fail: false
```

### GitHub Actions Security Rules
1. **Pin actions by SHA**, not tag: `actions/checkout@a81bbbf...` not `actions/checkout@v4`
2. **Never use `pull_request_target` with checkout** of PR code (code injection risk)
3. **Restrict `GITHUB_TOKEN` permissions** to minimum via `permissions:` block
4. **Never echo secrets** in logs; use `::add-mask::` for dynamic values
5. **Use environments** with required reviewers for production deploys
6. **Avoid `${{ github.event.*.body }}` in `run:` blocks** (injection via PR titles/descriptions)
7. **Use OIDC** for cloud provider auth instead of long-lived secrets

---

## GitLab CI Security

### Hardened Template
```yaml
stages:
  - scan
  - build
  - test
  - deploy

variables:
  DOCKER_CONTENT_TRUST: "1"
  TRIVY_SEVERITY: CRITICAL,HIGH

secrets-scan:
  stage: scan
  image: zricethezav/gitleaks:latest
  script:
    - gitleaks detect --source . --verbose --redact
  allow_failure: false

semgrep-sast:
  stage: scan
  image: semgrep/semgrep:latest
  script:
    - semgrep ci --config p/owasp-top-ten --config p/security-audit
  allow_failure: false

trivy-sca:
  stage: scan
  image: aquasec/trivy:latest
  script:
    - trivy fs --severity $TRIVY_SEVERITY --exit-code 1 .

container-scan:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - trivy image --severity $TRIVY_SEVERITY --exit-code 1 $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA

checkov-iac:
  stage: scan
  image: bridgecrew/checkov:latest
  script:
    - checkov -d . --framework terraform,kubernetes,dockerfile
  allow_failure: false
```

---

## Secrets Management

### Hierarchy of Secrets Storage (best → worst)
1. **Hardware Security Module (HSM)** — AWS CloudHSM, Azure Dedicated HSM
2. **Cloud KMS** — AWS KMS, GCP KMS, Azure Key Vault
3. **Secrets Manager** — HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager
4. **CI/CD Platform Secrets** — GitHub Secrets, GitLab CI Variables (masked + protected)
5. **Encrypted files in repo** — SOPS, git-crypt (acceptable for non-critical config)
6. ❌ **Environment variables in plain text** — NEVER
7. ❌ **Hardcoded in source** — NEVER

### Secret Rotation Strategy
| Secret Type | Rotation Frequency | Method |
|---|---|---|
| API keys | 90 days | Automated via secrets manager |
| Database credentials | 30 days | Dynamic secrets (Vault) |
| TLS certificates | Auto-renew 30 days before expiry | cert-manager / Let's Encrypt |
| SSH keys | 90 days | Automated rotation |
| Service account tokens | 24 hours | Short-lived OIDC tokens |

### Pre-commit Hooks Setup
```bash
# Install pre-commit
pip install pre-commit

# .pre-commit-config.yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.18.0
    hooks:
      - id: gitleaks
  - repo: https://github.com/Yelp/detect-secrets
    rev: v1.4.0
    hooks:
      - id: detect-secrets
        args: ['--baseline', '.secrets.baseline']
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: detect-private-key
```

---

## Supply Chain Security (SLSA)

### SLSA Levels
| Level | Requirements |
|---|---|
| **1** | Documentation of build process |
| **2** | Tamper-resistant build service, signed provenance |
| **3** | Hardened build platform, non-falsifiable provenance |
| **4** | Two-party review, hermetic/reproducible builds |

### Practical Steps
1. **Pin all dependencies** by hash in lockfiles
2. **Verify signatures** on all downloaded packages
3. **Use SBOM generation** (Syft, CycloneDX) in CI
4. **Sign container images** with cosign (Sigstore)
5. **Enforce image policies** with OPA/Kyverno in K8s
6. **Mirror critical dependencies** in private registry
7. **Enable dependency review** on PRs (GitHub, GitLab)

### SBOM Generation
```bash
# Generate SBOM with Syft
syft dir:. -o cyclonedx-json > sbom.json

# Scan SBOM for vulnerabilities
grype sbom:sbom.json --fail-on high
```

### Container Image Signing
```bash
# Sign with cosign (keyless via Sigstore)
cosign sign --yes $IMAGE_DIGEST

# Verify
cosign verify $IMAGE_DIGEST --certificate-identity=... --certificate-oidc-issuer=...
```

---

## Artifact Security

### Build Artifact Integrity
- Generate SHA256 checksums for all artifacts
- Store artifacts in immutable registries (ECR, GCR, Harbor)
- Tag by digest, not mutable tags
- Implement admission controllers to verify signatures before deploy

### Dependency Pinning Examples
```dockerfile
# Dockerfile — pin by digest
FROM node:20-slim@sha256:abc123...

# Python — pin exact versions with hashes
# requirements.txt
flask==3.0.0 --hash=sha256:abc123...

# Node — use lockfile integrity
# package-lock.json automatically includes integrity hashes
```
