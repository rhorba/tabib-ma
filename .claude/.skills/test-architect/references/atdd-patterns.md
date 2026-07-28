# Test Architect — ATDD Patterns & Test Strategy Reference

Load when designing a test strategy, writing BDD specs, or running an adversarial review.

---

## ATDD Workflow

```
1. DISCOVER   — Three Amigos: PM + Dev + QA surface examples before coding
2. FORMULATE  — Turn examples into executable specs (Gherkin / table-driven)
3. AUTOMATE   — Wire specs to test harness (Cucumber, pytest-bdd, SpecFlow)
4. DEVELOP    — Code until the spec passes (TDD inner loop)
5. REVIEW     — Adversarial pass: edge cases, failure modes, security
```

---

## Gherkin Pattern Library

### Happy Path
```gherkin
Feature: User login
  Scenario: Successful login with valid credentials
    Given a registered user with email "alice@example.com"
    And their password is "Secure!Pass1"
    When they submit the login form
    Then they are redirected to the dashboard
    And a session cookie is set
```

### Boundary / Equivalence
```gherkin
  Scenario Outline: Login lockout after repeated failures
    Given the account has <attempts> failed logins
    When they attempt login with wrong password
    Then the account is <status>

    Examples:
      | attempts | status  |
      | 9        | active  |
      | 10       | locked  |
```

### Error Path
```gherkin
  Scenario: Login with non-existent email
    Given no account exists for "ghost@example.com"
    When they attempt login
    Then the response is 401
    And the error message does not reveal whether the email exists
```

---

## Adversarial Test Checklist

### Input Manipulation
- [ ] Empty / null on all required fields
- [ ] Oversized inputs (> max length)
- [ ] SQL injection: `'; DROP TABLE users; --`
- [ ] XSS: `<script>alert(1)</script>`
- [ ] Path traversal: `../../../etc/passwd`
- [ ] Negative numbers / MAX_INT where quantity/price expected

### Concurrency
- [ ] Double-submit on payment / order forms
- [ ] Simultaneous password reset requests for same account
- [ ] Concurrent inventory decrement (race to negative stock)

### State Machine Abuse
- [ ] Skip steps in a multi-step flow
- [ ] Replay a completed order
- [ ] Reuse a single-use token
- [ ] Access paid features after downgrading plan

### Authorization Bypass
- [ ] Modify resource ID in URL to access another user's data
- [ ] Replay valid request with a different user's session
- [ ] Batch endpoint cross-tenant data leak

---

## Risk-Based Coverage Matrix

```
Risk = Probability of failure × Impact if it fails

| Feature / Path       | Probability | Impact   | Priority |
|----------------------|-------------|----------|----------|
| Authentication       | Medium      | Critical | MUST     |
| Payment processing   | Low         | Critical | MUST     |
| Password reset       | Low         | High     | Integration |
| Search / filtering   | Medium      | Low      | Unit only |
| Email notification   | Medium      | Med      | Integration |
```

---

## Release Gate Checklist

```
[ ] Unit + integration coverage >= 80%
[ ] All regression tests pass
[ ] No CRITICAL or HIGH open bugs
[ ] Adversarial checklist completed for auth / payments / data
[ ] Security scan clean (npm audit / Semgrep / Trivy)
[ ] Performance baseline not regressed > 10%
[ ] E2E smoke suite green on staging
```

---

## Test Pyramid Budget

| Layer | % of Effort | Rule |
|---|---|---|
| Unit | 60% | Every function with logic |
| Integration | 30% | API endpoints, DB queries, external calls |
| E2E | 10% | Top 5-10 critical user journeys only |

**Avoid the ice-cream cone**: too many E2E → slow, brittle suite.
