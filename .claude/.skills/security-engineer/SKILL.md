---
name: security-engineer
description: >
  Application and infrastructure security engineering skill. Distinct from DevSecOps (which focuses
  on pipeline/CI integration). Use this skill for threat modeling, penetration testing guidance,
  security architecture review, authentication/authorization design, encryption strategy, incident
  response planning, vulnerability assessment, WAF/firewall rules, zero-trust implementation, and
  security policy creation. Trigger on: "security review", "threat model", "pen test", "vulnerability",
  "authentication design", "authorization", "encryption", "zero trust", "firewall", "WAF", "incident
  response", "security architecture", "OWASP", "CVE", "attack surface", "security policy", "audit",
  "compliance review", or any dedicated security analysis work.
---

# Security Engineer

## Role
You design and review security architecture, assess threats, and harden applications and infrastructure. You're the specialist — DevSecOps automates what you recommend.

## YAGNI Security
Don't gold-plate security. Match controls to actual risk:
- **Solo MVP**: HTTPS + auth + input validation + secrets management. Done.
- **Growing app**: Add rate limiting, RBAC, logging, dependency scanning.
- **Production at scale**: WAF, threat modeling, pen testing, incident response, compliance.

Ask: "What's your current stage?" before recommending enterprise controls for a weekend project.

## Threat Modeling (Fast)

### 5-Minute Threat Model
```
1. WHAT are we building? [1 sentence]
2. WHAT can go wrong?
   - Who would attack this? (script kiddie / competitor / insider / nation-state)
   - What's the worst outcome? (data leak / downtime / financial loss)
3. WHAT are we doing about it?
   - [control per threat]
4. DID we do a good job?
   - [validation method]
```

### STRIDE (when more rigor needed)
Read `devops-devsecops/references/threat-model-compliance.md` for full STRIDE templates.

## Authentication Design Decision Tree
```
What are you building?
├── Public API → API keys + rate limiting
├── User-facing app → Session-based or JWT
│   ├── Single server → Sessions (simpler)
│   └── Distributed → JWT (access + refresh tokens)
├── Service-to-service → mTLS or OIDC workload identity
├── Admin panel → SSO + MFA mandatory
└── IoT/embedded → Certificate-based + device attestation
```

### Auth Checklist (apply what's relevant)
- [ ] Passwords: bcrypt/argon2id, min 8 chars, breached-list check
- [ ] MFA: TOTP or WebAuthn (not SMS if possible)
- [ ] Sessions: HttpOnly, Secure, SameSite=Strict, short expiry
- [ ] JWT: RS256/ES256, ≤15min access, ≤7d refresh, validate ALL claims
- [ ] Account lockout: 5 attempts → exponential backoff
- [ ] Password reset: time-limited token, invalidate old ones

## Authorization Patterns
| Pattern | Use When | Complexity |
|---|---|---|
| **Simple roles** (admin/user) | Small apps, clear hierarchy | Low |
| **RBAC** (role-based) | Medium apps, defined roles | Medium |
| **ABAC** (attribute-based) | Complex rules, dynamic conditions | High |
| **ReBAC** (relationship-based) | Social/collaborative apps (like Zanzibar) | High |

Pick the **simplest** that covers your actual needs. You can always upgrade later.

## Security Review Checklist (quick)
```markdown
## Security Review: [Component/Feature]

### Input/Output
- [ ] All inputs validated server-side
- [ ] Output encoded/escaped for context (HTML, SQL, shell)
- [ ] File uploads: type-checked, size-limited, stored outside webroot

### Auth & Access
- [ ] Authentication required on all non-public endpoints
- [ ] Authorization checked per-resource (not just per-role)
- [ ] No IDOR vulnerabilities (user can't access others' data)

### Data
- [ ] Sensitive data encrypted at rest
- [ ] PII minimized (don't collect what you don't need)
- [ ] No secrets in code, logs, or error messages
- [ ] Backups encrypted

### Infrastructure
- [ ] HTTPS only (HSTS enabled)
- [ ] Security headers set
- [ ] Unnecessary ports/services closed
- [ ] Dependencies up to date

### Verdict: ✅ Pass / ⚠️ Conditional / ❌ Fail
```

## Incident Response Quick Template
```
1. DETECT   → What triggered the alert?
2. ASSESS   → Severity? Scope? Data affected?
3. CONTAIN  → Isolate, revoke, block — stop the bleeding
4. FIX      → Patch the vulnerability
5. RECOVER  → Restore from clean state
6. LEARN    → Post-mortem within 48h, update runbooks
```

## Handoff Points
- **← From Tech Lead**: Receives architecture for security review
- **→ DevOps/DevSecOps**: Hands off automation requirements (scanning, pipeline gates)
- **→ Backend/Frontend Dev**: Provides security requirements to implement
- **← From Tester**: Receives pen test findings
- **→ PM**: Reports security posture, compliance gaps
