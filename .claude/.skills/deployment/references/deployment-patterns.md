# Deployment Engineer — Deployment Patterns & Release Reference

Load when executing a deployment, designing a release strategy, or setting up environments.

---

## Deployment Strategy Comparison

| Strategy | Downtime | Risk | Rollback Speed | Best For |
|---|---|---|---|---|
| Recreate | Yes | High | Fast (re-deploy old) | Dev / breaking changes |
| Rolling | None | Low | Fast (stop rollout) | Standard deploys |
| Blue-Green | None | Very Low | Instant (flip traffic) | Critical services |
| Canary | None | Lowest | Instant (cut traffic) | High-traffic / risky |
| Feature Flags | None | None | Toggle off | Gradual rollout |

---

## Rolling Deployment
```
1. Deploy to 1 instance → health check passes?
2. Deploy to next batch → health check passes?
3. Repeat until all instances updated
4. If any fails → stop rollout → rollback affected instances
```

## Blue-Green Deployment
```
1. Deploy new version to "green" (idle environment)
2. Run full smoke tests on green
3. Switch load balancer: blue → green (zero downtime)
4. Monitor error rate for 15 min
5. Green → If OK → decommission blue
   Green → If NOT OK → flip back to blue (instant rollback)
```

## Canary Deployment
```
1. Deploy new version to 5% of instances
2. Compare error rate + latency vs. baseline for 30 min
3. If good → increase to 25% → 50% → 100%
4. If bad → cut canary traffic to 0% instantly
```

---

## CI Gate (mandatory before every deploy)

```
[ ] CI pipeline GREEN on target branch
[ ] No CRITICAL / HIGH security scan findings
[ ] Coverage >= 80% (from CI report)
[ ] Staging smoke tests passed
[ ] DB migrations tested on staging data snapshot

If any item is FAIL → do not proceed with deployment.
```

---

## Pre-Deployment Checklist

```
Code
[ ] All tests passing (CI green)
[ ] Code reviewed and approved
[ ] No critical security findings (Trivy / Semgrep clean)
[ ] Environment variables documented and set in target env

Infrastructure
[ ] Target environment healthy
[ ] Sufficient resources (CPU, memory, disk > 20% free)
[ ] Dependencies available (DB, cache, queues, external APIs)
[ ] SSL/TLS certificates valid (> 14 days remaining)

Rollback
[ ] Rollback procedure documented and tested
[ ] Previous version image tagged and available
[ ] DB migration reversible or backward-compatible

Monitoring
[ ] Health check endpoint working
[ ] Alerting configured
[ ] Log aggregation active
```

---

## Quick Deploy Commands

### Docker + Compose
```bash
IMAGE_TAG=$(git rev-parse --short HEAD)
docker build -t app:$IMAGE_TAG .
docker push registry/app:$IMAGE_TAG
docker compose -f docker-compose.prod.yml up -d
```

### Kubernetes
```bash
# Deploy
kubectl set image deployment/app app=registry/app:$IMAGE_TAG -n production
kubectl rollout status deployment/app -n production

# Rollback (previous revision)
kubectl rollout undo deployment/app -n production

# Rollback to specific revision
kubectl rollout undo deployment/app --to-revision=3 -n production
```

### GitHub Actions Deploy Step
```yaml
- name: Deploy
  run: |
    IMAGE_TAG=${{ github.sha }}
    kubectl set image deployment/app app=registry/app:$IMAGE_TAG -n production
    kubectl rollout status deployment/app -n production --timeout=5m
```

---

## Post-Deployment Protocol

```
1. Health check: curl https://app.example.com/health → 200 OK
2. Smoke tests: run critical path tests against production
3. Monitor for 15 min:
   - Error rate stays < 1%
   - P95 latency within 20% of baseline
   - No unusual log patterns
4. Confirm CI still green (post-deploy checks)
5. Notify: "Deployed v[X] to production — all green"
6. Log: .logs/activity.md → [DATE] DEPLOY — v[X] to production | CI: green
```

---

## Environment Management

```
local → dev → staging → production
  └─ each environment has its own:
       - secrets (never shared between envs)
       - database
       - configuration

Staging must mirror production config at smaller scale.
Never use production data in staging without anonymization.
```

---

## Rollback Decision Tree

```
Post-deploy issue detected:
  Is it a data integrity / security issue?
    YES → rollback immediately, no discussion
  Is it affecting > 5% of users?
    YES → rollback immediately
  Is it a cosmetic / minor bug?
    NO → fix forward with hotfix PR
  Is CI now red because of the deploy?
    YES → rollback immediately
```

Rollback is not failure — shipping fast and recovering faster is the goal.
