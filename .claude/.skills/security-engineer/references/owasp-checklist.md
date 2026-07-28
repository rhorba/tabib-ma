# Security Engineer — OWASP & Security Review Checklist

Load this file when performing a dedicated security review, threat model, or compliance audit.

---

## OWASP Top 10 (2021) — Action Checklist

### A01 Broken Access Control
- [ ] All endpoints enforce auth before returning data
- [ ] Role-based access checked server-side on every request (not just UI-hidden)
- [ ] Direct object references use opaque IDs or authorization checks (IDOR)
- [ ] CORS policy explicit — no wildcard `*` in production
- [ ] JWT / session tokens validated server-side on every call

### A02 Cryptographic Failures
- [ ] No sensitive data (PII, credentials, tokens) stored in plaintext
- [ ] Passwords hashed with bcrypt / Argon2 (min cost factor 12)
- [ ] TLS 1.2+ enforced; TLS 1.0/1.1 disabled
- [ ] Secrets never committed to git — use `.env.example` + vault
- [ ] Database columns with PII encrypted at rest

### A03 Injection
- [ ] All SQL uses parameterized queries or ORM — no string concatenation
- [ ] NoSQL queries sanitized
- [ ] OS commands never built from user input
- [ ] Template engines use auto-escaping

### A04 Insecure Design
- [ ] Threat model documented for auth, payments, and data flows
- [ ] Rate limiting on all unauthenticated endpoints
- [ ] Sensitive flows (password reset, 2FA) have anti-automation controls

### A05 Security Misconfiguration
- [ ] Default credentials changed on all services
- [ ] Debug mode / stack traces disabled in production
- [ ] HTTP security headers set (see section below)
- [ ] Dependencies audited (`npm audit`, `pip-audit`, `trivy`)
- [ ] Container images run as non-root

### A06 Vulnerable Components
- [ ] `npm audit` / `pip-audit` / `trivy fs` run in CI
- [ ] No packages with known critical CVEs
- [ ] Dependabot / Renovate PRs enabled

### A07 Authentication Failures
- [ ] Account lockout after N failed attempts
- [ ] Password reset tokens: single-use, ≤ 15 min TTL, invalidated on use
- [ ] Session tokens: random, ≥ 128 bits, regenerated after login
- [ ] Cookies: `HttpOnly; Secure; SameSite=Strict`

### A08 Software and Data Integrity
- [ ] CI pipeline artifacts checksummed
- [ ] Deserialization of untrusted data avoided

### A09 Logging & Monitoring Failures
- [ ] All auth events logged (login, logout, failures, password change)
- [ ] Logs shipped to central system — no PII in log lines
- [ ] Alerts for repeated auth failures and large data exports

### A10 SSRF
- [ ] User-supplied URLs blocked from reaching internal networks
- [ ] Cloud metadata endpoints (169.254.169.254) blocked at network layer

---

## HTTP Security Headers

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; script-src 'self'
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), camera=(), microphone=()
```

---

## Threat Modeling Template (STRIDE)

```
Asset: [what are we protecting?]
Threat actors: [who might attack?]

| Threat              | Component      | Risk     | Mitigation              |
|---------------------|----------------|----------|-------------------------|
| Spoofing            | Auth endpoint  | High     | MFA + rate limiting     |
| Tampering           | Data in transit| High     | TLS + HMAC signing      |
| Repudiation         | Admin actions  | Med      | Immutable audit log     |
| Info disclosure     | API responses  | High     | Field-level auth        |
| DoS                 | Public endpoints| Med     | Rate limiting + CDN     |
| Elevation of priv.  | Role system    | Critical | Server-side RBAC        |
```

---

## Adversarial Review Checklist

### Auth flows
- [ ] Can an attacker reset another user's password by manipulating the token?
- [ ] Does the password reset endpoint leak whether an email exists?
- [ ] Does logout actually invalidate the server-side session?

### Payments
- [ ] Can price be manipulated in the client request?
- [ ] Are webhook signatures verified before processing?
- [ ] Is idempotency enforced to prevent double-charges?

### Data access
- [ ] Can a user access another user's data by changing an ID in the URL?
- [ ] Do bulk endpoints leak cross-tenant data?
- [ ] Are file upload paths traversal-safe?

---

## Compliance Quick Reference

| Standard | Key Control |
|---|---|
| GDPR | Consent, right to erasure, breach notification within 72hr |
| SOC 2 | Access control, monitoring, risk management |
| PCI-DSS | Never store CVV, tokenize PANs, quarterly scans |
| HIPAA | PHI encryption at rest + transit, audit logs, BAA with vendors |
