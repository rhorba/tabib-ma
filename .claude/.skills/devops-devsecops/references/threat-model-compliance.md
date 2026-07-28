# Threat Modeling & Compliance Reference

## Threat Modeling Frameworks

### STRIDE (Microsoft)
Use for: Systematic identification of threats per component.

| Category | Threat | Mitigation |
|---|---|---|
| **S**poofing | Attacker impersonates a user/service | Strong auth, MFA, mutual TLS |
| **T**ampering | Data modified in transit/at rest | Integrity checks, HMAC, digital signatures |
| **R**epudiation | User denies action without proof | Audit logging, non-repudiation (signed logs) |
| **I**nformation Disclosure | Data exposed to unauthorized party | Encryption, access controls, data classification |
| **D**enial of Service | Service made unavailable | Rate limiting, auto-scaling, CDN, DDoS protection |
| **E**levation of Privilege | User gains unauthorized access | Least privilege, input validation, sandboxing |

### STRIDE Threat Model Template
```markdown
## System: [Name]
### Architecture Overview
[Brief description + data flow diagram reference]

### Assets
1. [What are we protecting? User data, API keys, etc.]

### Trust Boundaries
1. [Where do trust levels change? Internet→DMZ, DMZ→internal, etc.]

### Threats (per component)

#### Component: [e.g., API Gateway]
| STRIDE | Threat | Likelihood | Impact | Risk | Mitigation | Status |
|--------|--------|-----------|--------|------|------------|--------|
| S | Forged JWT tokens | Medium | High | High | JWT signature verification, short expiry | ✅ Done |
| T | Modified request body | Low | High | Medium | Request signing, TLS | ✅ Done |
| I | API key leaked in logs | Medium | Critical | High | Log scrubbing, structured logging | ⚠️ In progress |
| D | API flooding | High | Medium | High | Rate limiting, WAF | ✅ Done |
| E | Broken access control | Medium | Critical | Critical | RBAC middleware, integration tests | ⚠️ In progress |

### Risk Matrix
| | Low Impact | Medium Impact | High Impact | Critical Impact |
|---|---|---|---|---|
| **High Likelihood** | Medium | High | Critical | Critical |
| **Medium Likelihood** | Low | Medium | High | Critical |
| **Low Likelihood** | Info | Low | Medium | High |
```

### DREAD (Risk Rating)
| Factor | Score (1-10) |
|---|---|
| **D**amage potential | How bad if exploited? |
| **R**eproducibility | How easy to reproduce? |
| **E**xploitability | How easy to exploit? |
| **A**ffected users | How many impacted? |
| **D**iscoverability | How easy to find? |

Risk = Average of all factors. Use to prioritize remediation.

### Attack Trees
```
Goal: Steal user credentials
├── Phishing attack
│   ├── Spear phishing email (likelihood: high)
│   └── Fake login page (likelihood: medium)
├── Exploit application vulnerability
│   ├── SQL injection to dump users table (likelihood: medium)
│   ├── XSS to steal session cookie (likelihood: medium)
│   └── SSRF to access internal auth service (likelihood: low)
├── Compromise infrastructure
│   ├── Exploit unpatched server (likelihood: low)
│   └── Access via leaked cloud credentials (likelihood: medium)
└── Social engineering
    ├── Impersonate IT support (likelihood: medium)
    └── Bribe/coerce insider (likelihood: low)
```

### MITRE ATT&CK Mapping
When documenting findings, map to ATT&CK techniques for consistency:
- **Initial Access**: T1190 (Exploit Public-Facing App), T1078 (Valid Accounts)
- **Execution**: T1059 (Command/Script Interpreter), T1203 (Client Execution)
- **Persistence**: T1098 (Account Manipulation), T1136 (Create Account)
- **Privilege Escalation**: T1068 (Exploitation), T1078 (Valid Accounts)
- **Defense Evasion**: T1070 (Indicator Removal), T1562 (Impair Defenses)
- **Credential Access**: T1110 (Brute Force), T1552 (Unsecured Credentials)
- **Lateral Movement**: T1021 (Remote Services), T1550 (Use Alternate Auth)
- **Exfiltration**: T1048 (Exfil Over Alternative Protocol), T1567 (Exfil to Cloud)

---

## Compliance Frameworks

### SOC 2 Type II
**Trust Service Criteria (TSC):**

| Category | Key Controls |
|---|---|
| **Security** (CC) | Access control, encryption, vulnerability management, incident response |
| **Availability** (A) | Uptime monitoring, disaster recovery, capacity planning |
| **Processing Integrity** (PI) | Input validation, error handling, QA testing |
| **Confidentiality** (C) | Data classification, encryption at rest/transit, access logging |
| **Privacy** (P) | Consent management, data minimization, retention policies, DSARs |

**SOC 2 DevSecOps Controls Checklist:**
- [ ] Version control with branch protection
- [ ] Code review required before merge
- [ ] Automated security scanning in CI/CD
- [ ] Vulnerability management with SLAs
- [ ] Secrets management (no plaintext secrets)
- [ ] Access reviews quarterly
- [ ] MFA enforced for all systems
- [ ] Encryption at rest and in transit
- [ ] Audit logging with 1-year retention
- [ ] Incident response plan tested annually
- [ ] Change management process documented
- [ ] Backup and disaster recovery tested
- [ ] Vendor security assessments
- [ ] Security awareness training annually

### PCI-DSS v4.0
**Relevant Requirements for DevSecOps:**

| Req | Description | Implementation |
|---|---|---|
| 2 | Secure system configurations | CIS benchmarks, IaC scanning |
| 3 | Protect stored account data | Encryption, tokenization, key management |
| 4 | Encrypt transmission | TLS 1.2+, no deprecated ciphers |
| 6 | Secure software development | SAST/DAST, secure SDLC, code review |
| 8 | Strong access controls | MFA, unique IDs, password policies |
| 10 | Log and monitor | Centralized logging, alerting, 1-year retention |
| 11 | Test security regularly | Quarterly scans, annual pen tests |
| 12 | Security policies | Documented and reviewed annually |

**PCI Scope Reduction**: Use tokenization, network segmentation, and P2PE to minimize CDE (Cardholder Data Environment).

### HIPAA
**Technical Safeguards:**
- Access controls with unique user IDs
- Encryption of ePHI at rest and in transit
- Audit controls and activity logging
- Integrity controls for ePHI
- Transmission security (TLS 1.2+)
- Automatic logoff
- BAAs (Business Associate Agreements) with all vendors

### ISO 27001:2022
**Annex A Controls (selected):**
- A.5: Information security policies
- A.6: Organization of information security
- A.7: Human resource security
- A.8: Asset management and classification
- A.9: Access control
- A.10: Cryptography
- A.12: Operations security
- A.13: Communications security
- A.14: System acquisition and development
- A.16: Incident management
- A.17: Business continuity

### NIST Cybersecurity Framework (CSF) 2.0
| Function | Activities |
|---|---|
| **Govern** | Risk management strategy, roles, policies |
| **Identify** | Asset inventory, risk assessment, data classification |
| **Protect** | Access control, training, data security, maintenance |
| **Detect** | Monitoring, anomaly detection, continuous assessment |
| **Respond** | Incident response, mitigation, communication |
| **Recover** | Recovery planning, improvements, communication |

---

## Security Assessment Templates

### Rapid Security Audit (30-minute)
```markdown
## Quick Security Audit: [System Name]
Date: [Date]
Auditor: [Name]

### 1. Authentication
- [ ] MFA enabled for all users?
- [ ] Password policy meets requirements?
- [ ] Session management secure?
- [ ] API auth implemented correctly?

### 2. Authorization
- [ ] RBAC/ABAC implemented?
- [ ] Least privilege enforced?
- [ ] No horizontal privilege escalation?

### 3. Data Protection
- [ ] Encryption at rest?
- [ ] Encryption in transit (TLS 1.2+)?
- [ ] No sensitive data in logs?
- [ ] PII handling compliant?

### 4. Infrastructure
- [ ] Firewall rules minimal?
- [ ] No public-facing admin interfaces?
- [ ] OS and dependencies patched?
- [ ] Container security configured?

### 5. Monitoring
- [ ] Security event logging?
- [ ] Alerting configured?
- [ ] Incident response plan exists?

### Findings Summary
| # | Finding | Severity | Remediation | Owner | Due |
|---|---------|----------|-------------|-------|-----|
| 1 | | | | | |
```

### Penetration Test Scope Template
```markdown
## Pen Test Scope: [Project Name]

### In Scope
- Application: [URLs, APIs]
- Infrastructure: [IP ranges, cloud accounts]
- Test types: [Black box / Gray box / White box]

### Out of Scope
- Production databases (use staging)
- Social engineering
- Physical security
- Third-party services

### Rules of Engagement
- Testing window: [Dates/times]
- Rate limiting: Max [X] requests/second
- No destructive testing
- Immediate notification for critical findings
- Emergency contact: [Name, phone]

### Deliverables
- Executive summary
- Technical findings with CVSS scores
- Proof of concept for each finding
- Remediation recommendations
- Retest after fixes
```

---

## Incident Response

### IR Playbook Template
```
1. DETECT — Alert triggered / report received
   - Verify the alert is real (not false positive)
   - Classify severity (P1-P4)
   - Assign incident commander

2. CONTAIN — Stop the bleeding
   - Isolate affected systems
   - Revoke compromised credentials
   - Block malicious IPs/domains
   - Preserve evidence (snapshots, logs)

3. ERADICATE — Remove the threat
   - Patch vulnerability
   - Remove malware/backdoors
   - Reset all potentially compromised credentials
   - Deploy updated configs

4. RECOVER — Return to normal
   - Restore from known-good backups
   - Gradual service restoration
   - Enhanced monitoring
   - Verify integrity

5. POST-MORTEM — Learn and improve
   - Timeline of events
   - Root cause analysis
   - What worked / what didn't
   - Action items with owners and deadlines
   - Update runbooks and detection rules
```
