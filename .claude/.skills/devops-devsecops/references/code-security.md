# Code Security Reference

## OWASP Top 10 (2021) — Detection & Remediation

### A01: Broken Access Control
**Detect with**: Semgrep rules `p/owasp-top-ten`, manual review of route guards
**Common patterns**:
- Missing authorization checks on endpoints
- IDOR (Insecure Direct Object Reference) — user can access other users' data by changing an ID
- Path traversal via unsanitized file paths
- CORS misconfiguration (`Access-Control-Allow-Origin: *`)
- Missing function-level access control

**Remediation**:
- Deny by default; every endpoint requires explicit authorization
- Use framework-level middleware (e.g., `@login_required`, `authorize` filters)
- Validate object ownership server-side: `if resource.owner_id != current_user.id: return 403`
- Use indirect references (UUIDs/slugs) instead of sequential IDs
- Implement RBAC or ABAC consistently

### A02: Cryptographic Failures
**Detect with**: Semgrep `p/secrets`, grep for hardcoded keys, Bandit (Python)
**Common patterns**:
- Storing passwords in plaintext or with weak hashing (MD5, SHA1)
- Using ECB mode, hardcoded IVs, or deprecated ciphers (DES, RC4)
- Transmitting sensitive data over HTTP
- Weak random number generation (`Math.random()` for tokens)

**Remediation**:
- Passwords: bcrypt (cost ≥12), scrypt, or Argon2id
- Encryption: AES-256-GCM or ChaCha20-Poly1305
- TLS 1.2+ everywhere; HSTS headers
- Cryptographic randomness: `secrets` (Python), `crypto.randomBytes` (Node), `SecureRandom` (Java)
- Key management: Vault, AWS KMS, GCP KMS — never in code

### A03: Injection
**Detect with**: Semgrep, CodeQL taint tracking, SonarQube
**Types**: SQL, NoSQL, OS command, LDAP, XPath, template injection

**SQL Injection remediation**:
```python
# VULNERABLE
cursor.execute(f"SELECT * FROM users WHERE id = {user_input}")

# SECURE — parameterized query
cursor.execute("SELECT * FROM users WHERE id = %s", (user_input,))
```

```javascript
// VULNERABLE
db.query(`SELECT * FROM users WHERE id = ${req.params.id}`);

// SECURE
db.query("SELECT * FROM users WHERE id = $1", [req.params.id]);
```

**Command Injection remediation**:
```python
# VULNERABLE
os.system(f"ping {user_input}")

# SECURE — use subprocess with list args, no shell=True
subprocess.run(["ping", "-c", "4", validated_host], check=True)
```

### A04: Insecure Design
**Detect with**: Threat modeling, architecture review
**Remediation**: Use secure design patterns:
- Rate limiting on all auth endpoints
- Account lockout with exponential backoff
- CAPTCHA for high-risk operations
- Transaction limits and velocity checks
- Segregation of duties

### A05: Security Misconfiguration
**Detect with**: Checkov, Trivy IaC scanning, cloud security posture tools
**Common issues**:
- Default credentials left active
- Debug mode in production
- Unnecessary ports/services exposed
- Missing security headers
- Directory listing enabled
- Stack traces returned to users

**Essential Security Headers**:
```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

### A06: Vulnerable and Outdated Components
**Detect with**: Snyk, Trivy, OWASP Dependency-Check, Dependabot
**Remediation**:
- Pin dependency versions (lockfiles)
- Automate dependency updates (Renovate, Dependabot)
- Monitor CVE databases (NVD, GitHub Advisory)
- Remove unused dependencies
- Use `npm audit`, `pip audit`, `bundle audit`

### A07: Identification and Authentication Failures
**Detect with**: Manual review, Semgrep auth rules
**Remediation**:
- MFA on all accounts
- Passwords: min 8 chars, check against breached lists (Have I Been Pwned API)
- Session management: HttpOnly, Secure, SameSite=Strict cookies
- Token expiration: access tokens ≤15 min, refresh tokens ≤7 days
- Account lockout after 5 failed attempts

### A08: Software and Data Integrity Failures
**Detect with**: SCA tools, CI pipeline audit
**Remediation**:
- Verify signatures/checksums on all dependencies
- Use lockfiles and integrity hashes
- Sign artifacts and container images (cosign, Sigstore)
- Protect CI/CD pipeline configurations
- Use SLSA framework for supply chain integrity

### A09: Security Logging and Monitoring Failures
**Detect with**: Architecture review, log audit
**What to log**: Auth events, access control failures, input validation failures, application errors, admin actions
**What NOT to log**: Passwords, tokens, PII, credit card numbers
**Remediation**:
- Structured logging (JSON) with correlation IDs
- Centralize logs (ELK, Splunk, CloudWatch)
- Alert on anomalies (brute force, privilege escalation)
- Retain logs ≥90 days (≥1 year for compliance)

### A10: Server-Side Request Forgery (SSRF)
**Detect with**: Semgrep, CodeQL
**Remediation**:
- Validate and allowlist URLs server-side
- Block requests to internal IPs (169.254.x.x, 10.x.x.x, 172.16-31.x.x, 192.168.x.x)
- Use network-level controls (firewall egress rules)
- Disable HTTP redirects or validate redirect targets

---

## Language-Specific Secure Coding

### Python
- Use `secrets` for tokens, not `random`
- Use `subprocess.run([...])` never `os.system()` or `shell=True`
- Use `defusedxml` instead of `xml.etree` for XML parsing
- Use `bleach` or `markupsafe` for HTML sanitization
- Enable `safety` and `bandit` in CI

### JavaScript/TypeScript
- Use `helmet` middleware for Express security headers
- Use `DOMPurify` for client-side HTML sanitization
- Avoid `eval()`, `new Function()`, `innerHTML`
- Use `crypto.randomUUID()` for IDs
- Enable strict CSP; avoid `unsafe-inline` and `unsafe-eval`
- Use `zod` or `joi` for input validation

### Go
- Use `html/template` (auto-escapes) not `text/template`
- Use `crypto/rand` not `math/rand`
- Use `sqlx` with parameterized queries
- Run `gosec` and `staticcheck` in CI
- Handle errors explicitly; never ignore them

### Java
- Use PreparedStatement for all SQL
- Use OWASP ESAPI or Spring Security for encoding
- Avoid Java deserialization of untrusted data
- Use `SecureRandom` not `Random`
- Enable SpotBugs with FindSecBugs plugin

---

## Input Validation Principles

1. **Validate on the server** — client validation is UX only
2. **Allowlist over denylist** — define what's valid, reject everything else
3. **Validate type, length, format, range** — in that order
4. **Canonicalize before validation** — normalize Unicode, decode URL encoding
5. **Use schema validation** — JSON Schema, Zod, Pydantic, Joi

---

## Authentication & Authorization Patterns

### JWT Best Practices
- Use RS256 or ES256 (asymmetric); avoid HS256 in distributed systems
- Set short expiration (15 min access, 7 day refresh)
- Include `iss`, `aud`, `exp`, `iat`, `jti` claims
- Validate ALL claims on every request
- Store refresh tokens server-side; access tokens in memory only
- Implement token revocation (blacklist or short-lived + rotation)

### API Security
- Use API keys for identification, OAuth2 for authorization
- Rate limit per key/IP: 100 req/min default, adjust per endpoint
- Use scoped API keys (read-only vs read-write)
- Log all API access with request IDs
- Version APIs; deprecate insecure versions
