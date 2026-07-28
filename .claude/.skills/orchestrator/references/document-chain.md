# Document-First Artifact Chain

## Overview

For medium-to-large features and new projects, follow a strict artifact chain where each document
feeds the next. This ensures zero context loss and aligned understanding.

```
PRD (what & why) → Architecture (how) → Epics & Stories (what to build) → Implementation
```

**YAGNI**: Skip the chain for small tasks (bug fixes, quick features). Use it for:
- New projects
- Large features (3+ days of work)
- Features with multiple specialists involved
- Anything with security, compliance, or data model implications

---

## Artifact 1: Product Requirements Document (PRD)

**Owner**: Project Manager (with input from user)
**Save as**: `docs/prd-[feature-name].md`

```markdown
# PRD: [Feature Name]
**Version**: 1.0 | **Date**: [date] | **Author**: PM | **Status**: Draft / Approved

## 1. Problem Statement
[What problem are we solving? Who has this problem? 2-3 sentences max.]

## 2. Goals & Success Metrics
| Goal | Metric | Target |
|---|---|---|
| [goal 1] | [how we measure] | [number] |
| [goal 2] | [how we measure] | [number] |

## 3. User Stories
As a [user], I want to [action], so that [benefit].
- [ ] Story 1: ...
- [ ] Story 2: ...
- [ ] Story 3: ...

## 4. Scope
### In Scope
- [feature/capability 1]
- [feature/capability 2]

### Out of Scope
- [explicitly excluded 1]
- [explicitly excluded 2]

## 5. Requirements
### Functional
- FR-1: [requirement]
- FR-2: [requirement]

### Non-Functional
- NFR-1: Performance — [target]
- NFR-2: Security — [requirement]
- NFR-3: Accessibility — [standard]

## 6. Constraints & Assumptions
- [constraint 1]
- [assumption 1]

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| [risk] | H/M/L | H/M/L | [action] |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | [date] |
| Architecture Done | [date] |
| Implementation Start | [date] |
| MVP Ready | [date] |
```

### PRD Validation Checklist
Before approving:
- [ ] Problem clearly stated (not a solution disguised as a problem)
- [ ] Success metrics are measurable
- [ ] Scope has explicit "out of scope" items
- [ ] User stories follow As a/I want/So that format
- [ ] Requirements are testable (can write acceptance criteria)
- [ ] Risks identified with mitigations

---

## Artifact 2: Architecture Document

**Owner**: Tech Lead (with input from DBA, Security Engineer)
**Depends on**: Approved PRD
**Save as**: `docs/architecture-[feature-name].md`

```markdown
# Architecture: [Feature Name]
**PRD Reference**: docs/prd-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: Tech Lead

## 1. Overview
[1-2 sentences: what this architecture covers and the chosen approach]

## 2. Architecture Decision Records
### ADR-1: [Decision Title]
- **Context**: [why we need to decide]
- **Decision**: [what we chose]
- **Alternatives**: [what we rejected and why]
- **Consequences**: [what changes because of this]

## 3. System Design
[Text-based diagram of components and data flow]
```
[Client] → [API Gateway] → [Service] → [Database]
                                ↓
                          [External API]
```

## 4. Data Model
[Key entities and relationships — hand off to DBA for full schema]
```
User ──1:N──> Post ──1:N──> Comment
User ──N:N──> Role (via user_roles)
```

## 5. API Design
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/[resource] | Create | Required |
| GET | /api/v1/[resource] | List | Required |
| GET | /api/v1/[resource]/:id | Get one | Required |
| PUT | /api/v1/[resource]/:id | Update | Owner |
| DELETE | /api/v1/[resource]/:id | Delete | Admin |

## 6. Security Considerations
[From Security Engineer review]
- Authentication: [method]
- Authorization: [model]
- Data protection: [encryption, PII handling]
- Key risks: [from threat model]

## 7. Infrastructure
- Hosting: [where]
- Database: [what]
- CI/CD: [pipeline]
- Monitoring: [tools]

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| [risk] | [action] | [who] |
```

### Architecture Validation Checklist
- [ ] Every PRD requirement has an architectural solution
- [ ] ADRs document all significant choices
- [ ] Data model supports all user stories
- [ ] API design covers all functional requirements
- [ ] Security requirements addressed
- [ ] NFRs have architectural support (caching, scaling, etc.)
- [ ] No over-engineering (YAGNI check)

---

## Artifact 3: Epics & Stories

**Owner**: Scrum Master (with input from Tech Lead)
**Depends on**: Approved Architecture
**Save as**: `docs/stories-[feature-name].md`

```markdown
# Stories: [Feature Name]
**PRD**: docs/prd-[feature-name].md
**Architecture**: docs/architecture-[feature-name].md

## Epic 1: [Epic Name]
[1-sentence description — what this epic delivers]

### Story 1.1: [Story Title]
**Priority**: Must / Should / Could
**Size**: S / M / L
**Specialist**: [Backend Dev / Frontend Dev / etc.]

**Description**:
As a [user], I want to [action], so that [benefit].

**Acceptance Criteria** (from Test Architect — ATDD):
```gherkin
Given [context]
When [action]
Then [expected result]
```

**Technical Notes** (from Architecture):
- Uses [API endpoint] from architecture doc
- Touches [data model / component]
- Security: [relevant requirement]

**Dependencies**: [other stories this depends on]

---

### Story 1.2: [Story Title]
...

## Epic 2: [Epic Name]
...

## Sprint Allocation
| Sprint | Stories | Estimated Effort |
|---|---|---|
| Sprint 1 | 1.1, 1.2, 1.3 | [X days] |
| Sprint 2 | 2.1, 2.2 | [X days] |
```

### Story Validation Checklist
- [ ] Every PRD requirement maps to at least one story
- [ ] Every story has testable acceptance criteria
- [ ] Dependencies are identified and ordered correctly
- [ ] Sizes are realistic (nothing larger than L — split if bigger)
- [ ] Architecture decisions are referenced in technical notes
- [ ] Security requirements are reflected in relevant stories

---

## Artifact 4: System Design

**Owner**: System Designer
**Depends on**: Approved PRD
**Save as**: `docs/system-design-[feature-name].md`

```markdown
# System Design: [Project Name]
**PRD Reference**: docs/prd-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | ___% SLA | |
| Latency (p99) | ___ms | |
| Throughput | ___ RPS | |
| Data Volume | ___ GB/day | |
| Retention | ___ days | |
| Recovery (RTO) | ___ min | |
| Recovery (RPO) | ___ min | |

## 2. Component Topology
[Text-based diagram of system components and data flow]
```
[Clients: Web / Mobile / API]
        ↓ HTTPS
[CDN / WAF]  ←── static assets, DDoS protection
        ↓
[Load Balancer]
        ↓
[API Gateway]  ←── auth, rate limiting, routing
        ↓
[Service Layer]
  ├── [Service A] → [DB A]
  └── [Service B] → [Cache] → [DB B]
        ↓
[Observability: Logs → Metrics → Traces]
```

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| [system] | REST / Queue / Event | [why] |

## 4. Scalability Strategy
- Scaling approach: [vertical / horizontal / auto-scale]
- Cache strategy: [none / Redis / CDN]
- Queue strategy: [none / needed for X]

## 5. System Design Decision Records
### SDR-1: [Decision Title]
- **NFR Driver**: [which NFR drives this decision]
- **Decision**: [chosen option and why]
- **Alternatives**: [what was rejected and why]
- **Re-evaluate when**: [scale / load / team size trigger]
```

### System Design Validation Checklist
- [ ] All NFRs captured with measurable targets
- [ ] Topology fits current project scale (YAGNI — no over-engineering)
- [ ] Data flow covers read and write paths
- [ ] Integration patterns chosen with justification
- [ ] SDRs document all key decisions

---

## Artifact 5: Security Baseline

**Owner**: Security Engineer
**Depends on**: Approved Architecture
**Save as**: `docs/security-[feature-name].md`

```markdown
# Security Baseline: [Project Name]
**Architecture Reference**: docs/architecture-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** [1 sentence]
- **Who would attack it?** [script kiddie / competitor / insider / nation-state]
- **Worst outcome?** [data leak / downtime / financial fraud]

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | [component] | [control] | TODO |
| Tampering | [component] | [control] | TODO |
| Repudiation | [component] | [control] | TODO |
| Info Disclosure | [component] | [control] | TODO |
| DoS | [component] | [control] | TODO |
| Elevation of Privilege | [component] | [control] | TODO |

## 3. Authentication Strategy
- **Type**: [Session / JWT / OAuth2 / mTLS / API keys]
- **MFA**: [required / optional / not needed — justify]
- **Password policy**: [min length, breach-list check]
- **Session management**: [timeout, HttpOnly, SameSite]

## 4. Authorization Model
- **Pattern**: [Simple roles / RBAC / ABAC]
- **Roles defined**: [list roles]
- **Resource-level checks**: [yes — per-object / no]

## 5. Data Protection
- **PII fields**: [list fields]
- **Encryption at rest**: [yes / no — method]
- **Encryption in transit**: [HTTPS enforced, HSTS]
- **Secrets management**: [env vars / vault / secret manager]

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side
- [ ] Output encoded for context (HTML, SQL, shell)
- [ ] No secrets in code, logs, or error messages
- [ ] HTTPS only, security headers configured
- [ ] Dependencies scanned in CI (SCA)
```

### Security Validation Checklist
- [ ] Threat model completed and top risks addressed
- [ ] Auth strategy chosen and justified
- [ ] Authorization model defined with roles
- [ ] PII fields identified with protection plan
- [ ] Security requirements handed off to dev team

---

## Artifact 6: Database Design

**Owner**: DBA
**Depends on**: Approved Architecture
**Save as**: `docs/database-[feature-name].md`

```markdown
# Database Design: [Project Name]
**Architecture Reference**: docs/architecture-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: DBA

## 1. Database Selection
- **Engine**: [PostgreSQL / MySQL / MongoDB / etc.]
- **Rationale**: [why this engine — YAGNI default: PostgreSQL]
- **Hosting**: [managed / self-hosted]

## 2. Entity-Relationship Model
```
[Entity A] ──1:N──> [Entity B]
[Entity B] ──N:N──> [Entity C] (via [junction_table])
```

## 3. Schema Design
```sql
-- Table: [table_name]
CREATE TABLE [table_name] (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  [field]     [TYPE] NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| [table] | idx_[name] | [col1, col2] | [WHERE clause it covers] |

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| 001_initial_schema.sql | [description] | Yes |

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| [use case] | SELECT by [col] | [index name] |

## 7. Sensitive Data
- Columns requiring encryption: [list]
- Row-level security needed: [yes / no]
```

### Database Validation Checklist
- [ ] Engine choice justified (YAGNI)
- [ ] All PRD entities modeled with relationships
- [ ] Schema in 3NF (denormalization only if measured bottleneck)
- [ ] Indexes on foreign keys + frequent WHERE columns only
- [ ] Migration files include rollback
- [ ] Sensitive columns identified for encryption

---

## Artifact 7: UX Foundation

**Owner**: UX Designer
**Depends on**: Approved PRD
**Save as**: `docs/ux-[feature-name].md`

```markdown
# UX Foundation: [Project Name]
**PRD Reference**: docs/prd-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: UX Designer

## 1. User Personas (minimal — YAGNI)
| Persona | Role | Goal | Pain Point |
|---|---|---|---|
| [name] | [who they are] | [what they want to achieve] | [what frustrates them] |

## 2. Information Architecture / Site Map
```
[App Root]
├── [Section A]
│   ├── [Page 1]
│   └── [Page 2]
└── [Section B]
    └── [Page 3]
```

## 3. Core User Flows (top 3 journeys)
### Flow 1: [Flow Name]
```
[Start State] → [Step 1] → [Decision?] → [Step 2] → [End / Success]
                                ↓ No
                          [Error / Alt path] → [Recovery]
```

## 4. Key Screen Wireframes (text-based)
### Screen: [Screen Name]
```
┌─────────────────────────────┐
│ [Header / Navigation]       │
├─────────────────────────────┤
│ [Primary Content Area]      │
│                             │
│   [Primary CTA Button]      │
├─────────────────────────────┤
│ [Footer]                    │
└─────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| [screen name] | [message/illustration] | [spinner/skeleton] | [error msg] | [feedback] |
```

### UX Validation Checklist
- [ ] Personas match PRD target users (no unnecessary depth — YAGNI)
- [ ] All PRD user stories map to a flow
- [ ] Wireframes cover happy path + at least one error state
- [ ] Navigation hierarchy is clear and shallow

---

## Artifact 8: UI Foundation

**Owner**: UI Designer
**Depends on**: Approved UX Foundation
**Save as**: `docs/ui-[feature-name].md`

```markdown
# UI Foundation: [Project Name]
**UX Reference**: docs/ux-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: [Custom design tokens / UI Framework: Tailwind / MUI / Shadcn / etc.]
- **Rationale**: [YAGNI — use a framework unless brand requires custom]

## 2. Design Tokens (fill in if custom; skip if using framework)
```css
/* Colors */
--color-primary:     #[hex];
--color-secondary:   #[hex];
--color-background:  #[hex];
--color-surface:     #[hex];
--color-error:       #[hex];
--color-text:        #[hex];
--color-text-muted:  #[hex];

/* Typography */
--font-family:   [font stack];
--font-size-sm:  [size];
--font-size-md:  [size];
--font-size-lg:  [size];
--font-size-xl:  [size];

/* Spacing scale */
--spacing-xs: [value];  --spacing-sm: [value];
--spacing-md: [value];  --spacing-lg: [value];
--spacing-xl: [value];
```

## 3. Component Inventory
| Component | Reuse Existing | Build New | Notes |
|---|---|---|---|
| Button | [framework component] | No | primary / secondary variants |
| [component] | [yes/no] | [yes/no] | [notes] |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | [adaptations] |
| Tablet | 768–1024px | [adaptations] |
| Desktop | > 1024px | [base layout] |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 for normal text, 3:1 for large)
- Focus indicators: visible on all interactive elements
- Semantic HTML first; ARIA only where native semantics are insufficient
```

### UI Validation Checklist
- [ ] Design approach chosen (framework vs custom — YAGNI justified)
- [ ] Tokens/framework covers all UX wireframe screens
- [ ] Component inventory complete (no surprises during execute)
- [ ] Responsive strategy defined for all breakpoints
- [ ] Accessibility baseline confirmed

---

## Artifact 9: Test Strategy

**Owner**: Test Architect
**Depends on**: Approved Stories
**Save as**: `docs/test-strategy-[feature-name].md`

```markdown
# Test Strategy: [Project Name]
**Stories Reference**: docs/stories-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: Test Architect

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Test Level |
|---|---|---|---|---|
| [component] | H/M/L | H/M/L | H/M/L | Maximum / High / Standard / Minimal |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | [jest / pytest / etc.] |
| Integration | ≥ 40% of API + DB layer | [supertest / testcontainers / etc.] |
| E2E | Critical happy paths only | [playwright / cypress] |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below |

## 3. ATDD Acceptance Scenarios (critical paths)
```gherkin
Feature: [Feature Name]

  Scenario: [Happy path title]
    Given [starting context]
    When [user action]
    Then [expected outcome]

  Scenario: [Error/edge path title]
    Given [starting context]
    When [invalid or edge action]
    Then [error handled gracefully]
```

## 4. Adversarial Checklist (high-risk components only)
- [ ] Input abuse: empty, oversized, malformed, unicode edge cases
- [ ] Auth abuse: unauthenticated access, privilege escalation, token replay
- [ ] Race conditions: concurrent writes, double-submit prevention
- [ ] Business logic: boundary values, invalid state transitions

## 5. Release Gate Criteria
- [ ] All acceptance scenarios pass
- [ ] Combined unit + integration coverage ≥ 80%
- [ ] No critical/high security findings open
- [ ] E2E happy path passes (and recorded if UI project)
```

### Test Strategy Validation Checklist
- [ ] Every story maps to at least one acceptance scenario
- [ ] Coverage gate ≥ 80% confirmed and CI-enforced
- [ ] Adversarial review planned for high-risk components
- [ ] Release gate criteria documented and agreed with user

---

## Artifact 10: DevOps Foundation

**Owner**: DevOps/DevSecOps
**Depends on**: Approved Architecture + Security Baseline
**Save as**: `docs/devops-[feature-name].md`

```markdown
# DevOps Foundation: [Project Name]
**Architecture**: docs/architecture-[feature-name].md
**Security**: docs/security-[feature-name].md
**Version**: 1.0 | **Date**: [date] | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development | Manual |
| staging | QA / Preview | PR merge to main |
| production | Live users | Manual tag / approved release |

## 2. CI Pipeline (GitHub Actions / GitLab CI / etc.)
```yaml
# Minimum viable pipeline stages:
stages:
  - lint            # code style + type checking
  - test            # unit + integration (fail CI if coverage < 80%)
  - security-scan   # SAST (Semgrep), SCA (Trivy), secrets (Gitleaks)
  - build           # Docker image or artifact
  - deploy-staging  # auto on PR merge
  - deploy-prod     # manual approval gate
```

## 3. Infrastructure
- **Hosting**: [cloud provider / VPS / managed platform]
- **Compute**: [containers / serverless / VM]
- **Database**: [managed / self-hosted]
- **Secrets**: [env vars / vault / cloud secret manager]
- **Monitoring**: [logging / metrics / tracing tool]

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities | Critical findings |
| Trivy | SCA — dependency CVEs | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup (if containerized)
```dockerfile
FROM [base-image]:[pinned-version]
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE [port]
USER node
CMD ["node", "dist/main.js"]
```

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | [tool] | Error rate > X/min |
| Metrics | [tool] | Latency p99 > Xms |
| Uptime | [tool] | SLO breach |
```

### DevOps Validation Checklist
- [ ] All 3 environments defined with deploy triggers
- [ ] CI pipeline covers lint + test (with coverage gate) + security scan + build + deploy
- [ ] Coverage gate configured (< 80% fails CI)
- [ ] Secrets management strategy confirmed (no hardcoded secrets)
- [ ] Monitoring baseline defined with alert thresholds

---

## Artifact Chain Workflow

### For New Projects — FIRST SESSION FOUNDATION RULE

> **MANDATORY**: The first session of every new project generates ALL expert docs before any code is written. Every active specialist produces and saves their doc to `docs/`. The session ends with a commit and push of all docs.

```
SESSION 1 — Foundation (all docs, no code):

  1. PM → docs/prd-[name].md
     User approves → 📝 Log: MILESTONE "PRD approved" → .logs/activity.md

  2. System Designer → docs/system-design-[name].md
     User approves → 📝 Log: MILESTONE "System design approved"

  3. Software Architect → docs/architecture-[name].md
     User approves → 📝 Log: MILESTONE "Architecture approved"
                   → 📝 Log: ARCHITECTURE decisions → .logs/decisions.md

  4. Security Engineer → docs/security-[name].md
     User approves → 📝 Log: MILESTONE "Security baseline approved"

  5. DBA → docs/database-[name].md
     User approves → 📝 Log: MILESTONE "Database design approved"

  6. UX Designer → docs/ux-[name].md
     User approves → 📝 Log: MILESTONE "UX foundation approved"

  7. UI Designer → docs/ui-[name].md
     User approves → 📝 Log: MILESTONE "UI foundation approved"

  8. Test Architect → docs/test-strategy-[name].md
     User approves → 📝 Log: MILESTONE "Test strategy approved"

  9. DevOps/DevSecOps → docs/devops-[name].md
     User approves → 📝 Log: MILESTONE "DevOps foundation approved"

 10. Scrum Master + Test Architect → docs/stories-[name].md
     User approves → 📝 Log: MILESTONE "Stories ready"

 11. Commit ALL docs and push:
     git add docs/
     git commit -m "docs: foundation documents for [project-name]"
     git push origin <branch>
     📝 Log: PUSH → .logs/activity.md

SESSION 2+ — Execution (follow stories, batch by batch):
  → Normal Execute workflow. Each story references its PRD requirement + architecture decision.
```

### For Large Features (add to existing project)
```
1. PM creates mini-PRD (sections 1-5 only) → User approves
2. System Designer notes topology delta (if topology changes) → User approves
3. Tech Lead creates architecture delta (ADR only, what changes) → User approves
4. Security Engineer reviews security impact → User approves
5. DBA notes schema delta (new tables/migrations only) → User approves
6. Scrum Master + Test Architect create stories → User approves
7. git add docs/ && git commit -m "docs: [feature-name] planning docs" && git push origin <branch>
8. Execute
```

### For Medium Features (2-3 days)
```
1. Skip PRD — PM writes a 3-line scope note
2. Tech Lead makes one ADR (no full arch doc)
3. Scrum Master creates stories directly
4. Commit scope note + stories, push, then Execute
```

### For Small Tasks (< 1 day)
```
Skip the chain entirely. Go straight to Execute.
```

---

## Traceability

Every story should trace back to a PRD requirement and forward to a test:

```
PRD Requirement → Architecture Decision → Story → Acceptance Test → Code
     FR-1      →      ADR-1            → S1.1  →   Scenario 1   → auth.ts
```

This chain ensures nothing is built without a reason, nothing is untested, and nothing is undocumented.
