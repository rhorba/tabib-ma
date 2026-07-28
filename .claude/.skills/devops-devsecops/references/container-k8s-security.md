# Container & Kubernetes Security Reference

## Dockerfile Hardening

### Complete Secure Dockerfile Template
```dockerfile
# === Build Stage ===
FROM python:3.12-slim@sha256:<pin-digest> AS builder

WORKDIR /build

# Install build deps in isolated stage
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# === Runtime Stage ===
FROM python:3.12-slim@sha256:<pin-digest> AS runtime

# Security labels
LABEL maintainer="security@company.com" \
      org.opencontainers.image.source="https://github.com/org/repo" \
      org.opencontainers.image.description="App description"

# Install only runtime OS deps, then clean up
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      ca-certificates \
      tini && \
    apt-get purge -y --auto-remove && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser -g 1001 && \
    useradd -r -g appuser -u 1001 -d /app -s /sbin/nologin appuser

WORKDIR /app

# Copy only built dependencies from builder
COPY --from=builder /install /usr/local

# Copy app code with correct ownership
COPY --chown=appuser:appuser . .

# Remove write permissions where not needed
RUN chmod -R a-w /app && \
    mkdir -p /app/tmp && chown appuser:appuser /app/tmp

# Drop to non-root
USER 1001

# Use tini as init system (handles signals properly)
ENTRYPOINT ["tini", "--"]

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

EXPOSE 8000
CMD ["gunicorn", "--bind", "0.0.0.0:8000", "--workers", "4", "app:app"]
```

### Dockerfile Security Checklist
- [ ] Multi-stage build (separate build and runtime)
- [ ] Base image pinned by digest (not `:latest`)
- [ ] Minimal base image (`-slim`, `-alpine`, or distroless)
- [ ] Non-root USER (numeric UID, not name)
- [ ] No secrets in build args or layers
- [ ] `--no-cache-dir` for pip, `--no-cache` for apk
- [ ] HEALTHCHECK defined
- [ ] Only necessary ports EXPOSE'd
- [ ] `.dockerignore` excludes `.git`, `.env`, secrets, tests
- [ ] tini or dumb-init as PID 1
- [ ] Read-only filesystem where possible

### .dockerignore Template
```
.git
.gitignore
.env
.env.*
*.md
LICENSE
docker-compose*.yml
Makefile
tests/
__pycache__/
*.pyc
.pytest_cache/
node_modules/
.npm/
coverage/
.secrets*
*.key
*.pem
```

---

## Container Image Scanning

### Trivy Scanning Commands
```bash
# Scan image for vulnerabilities
trivy image --severity CRITICAL,HIGH myapp:latest

# Scan with SBOM output
trivy image --format cyclonedx -o sbom.json myapp:latest

# Scan filesystem (source code)
trivy fs --security-checks vuln,secret,config .

# Scan Kubernetes cluster
trivy k8s --report summary cluster

# Scan Terraform
trivy config --severity CRITICAL,HIGH ./terraform/
```

### Grype Alternative
```bash
# Scan image
grype myapp:latest --fail-on high

# Scan from SBOM
syft myapp:latest -o cyclonedx-json | grype --fail-on high
```

---

## Kubernetes Security

### Pod Security Standards (PSS)

**Restricted** (production workloads — use this by default):
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true
    runAsUser: 1001
    runAsGroup: 1001
    fsGroup: 1001
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: app
      image: myapp@sha256:<digest>
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop: ["ALL"]
        runAsNonRoot: true
        runAsUser: 1001
      resources:
        limits:
          cpu: "500m"
          memory: "256Mi"
        requests:
          cpu: "100m"
          memory: "128Mi"
      volumeMounts:
        - name: tmp
          mountPath: /tmp
  volumes:
    - name: tmp
      emptyDir:
        sizeLimit: 100Mi
```

### Network Policies

**Default deny all:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

**Allow specific traffic:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-app-to-db
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
    - Egress
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - port: 5432
          protocol: TCP
```

### RBAC Least Privilege
```yaml
# Service account per workload
apiVersion: v1
kind: ServiceAccount
metadata:
  name: app-sa
  namespace: production
automountServiceAccountToken: false
---
# Role with minimum permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: app-role
  namespace: production
rules:
  - apiGroups: [""]
    resources: ["configmaps"]
    resourceNames: ["app-config"]  # Named resources only
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: app-rolebinding
  namespace: production
subjects:
  - kind: ServiceAccount
    name: app-sa
roleRef:
  kind: Role
  name: app-role
  apiGroup: rbac.authorization.k8s.io
```

### K8s Security Checklist
- [ ] Pod Security Standards enforced (Restricted mode)
- [ ] Network policies: default deny, explicit allow
- [ ] RBAC: no cluster-admin for workloads, namespace-scoped roles
- [ ] Service accounts: one per workload, automount disabled
- [ ] Secrets: encrypted at rest (EncryptionConfiguration), use external secrets operator
- [ ] Images: pulled by digest, from private registry, signed
- [ ] Resource limits: set on all containers
- [ ] Admission controllers: OPA Gatekeeper or Kyverno for policy enforcement
- [ ] Audit logging enabled on API server
- [ ] etcd encrypted and access-restricted
- [ ] Dashboard disabled or behind strong auth
- [ ] Node OS hardened (CIS benchmarks via kube-bench)

### Runtime Security with Falco
```yaml
# Example Falco rule: detect shell in container
- rule: Terminal shell in container
  desc: Detect shell started in a container
  condition: >
    spawned_process and container and
    proc.name in (bash, sh, zsh, ksh) and
    not proc.pname in (cron, supervisord)
  output: >
    Shell spawned in container
    (user=%user.name container=%container.name shell=%proc.name
     parent=%proc.pname cmdline=%proc.cmdline image=%container.image.repository)
  priority: WARNING
  tags: [container, shell, mitre_execution]
```

---

## Docker Compose Security
```yaml
version: "3.9"
services:
  app:
    image: myapp@sha256:<digest>
    read_only: true
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    tmpfs:
      - /tmp:noexec,nosuid,size=100m
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 256M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 3s
      retries: 3
    networks:
      - internal
    # Never use privileged, host network, or pid: host

networks:
  internal:
    driver: bridge
    internal: true  # No external access by default
```

---

## Container Registry Security
- Enable vulnerability scanning (ECR, GCR, Harbor all support it)
- Enable image signing and verification
- Use immutable tags in production
- Implement retention policies (delete untagged images after 30 days)
- Restrict push access to CI service accounts only
- Pull images through a proxy/mirror for air-gapped environments
