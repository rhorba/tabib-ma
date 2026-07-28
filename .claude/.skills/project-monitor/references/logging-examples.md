# Logging Examples — How to Log Efficiently

## Golden Rule: Log in the SAME tool call as the action

Don't make separate tool calls for logging. Append the log entry at the end of the bash command that performs the action.

---

## Example: Task Completion + Log in One Call

```bash
# Create the migration file AND log it in one command
cat > db/migrations/001_create_users.sql << 'EOF'
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
EOF

# Log the completion (same tool call)
cat >> .logs/activity.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') COMPLETED — Created users table migration
- **Specialist**: DBA
- **Summary**: Created migration 001_create_users with id, email, created_at
- **Status**: resolved
- **Impact**: medium
---
LOGEOF
```

## Example: Decision Log

```bash
cat >> .logs/decisions.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') ARCHITECTURE — Database selection
- **Specialist**: Tech Lead
- **Summary**: Chose PostgreSQL over MongoDB
- **Options considered**: PostgreSQL (chosen), MongoDB, SQLite
- **Rationale**: Relational data, ACID transactions, team knows SQL
- **Status**: resolved
- **Impact**: high
---
LOGEOF
```

## Example: Issue Found During Testing

```bash
# Run tests AND log any failure
npm test 2>&1 | tee /tmp/test-output.txt
if [ $? -ne 0 ]; then
  cat >> .logs/issues.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') BUG — Test failure in auth module
- **Specialist**: Tester
- **Summary**: Login endpoint returns 500 when email has uppercase chars
- **Location**: src/auth/login.test.ts
- **Severity**: high
- **Status**: open
- **Impact**: high
---
LOGEOF
fi
```

## Example: Session Start (check for previous sessions)

```bash
# Initialize logs if they don't exist
if [ ! -d ".logs" ]; then
  bash skills/project-monitor/templates/init-logs.sh
fi

# Log session start
cat >> .logs/sessions.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') SESSION_START
- **Context**: [what user wants to work on]
- **Resuming from**: [last session summary or "Fresh start"]
- **Plan**: [what we'll do this session]
---
LOGEOF
```

## Example: Session End

```bash
cat >> .logs/sessions.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') SESSION_END
- **Completed**: [list of tasks done]
- **In progress**: [what was started but not finished]
- **Blocked**: [any blockers]
- **Next session**: [what to do next]
- **Open issues**: [count]
- **Open risks**: [count]
---
LOGEOF
```

## Example: Blocker Hit

```bash
cat >> .logs/issues.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') BLOCKER — Missing API credentials
- **Specialist**: Backend Dev
- **Summary**: Cannot integrate payment API — no Stripe keys provided
- **Workaround**: Using mock payment responses for development
- **Status**: open
- **Impact**: critical
---
LOGEOF
```

## Example: Risk Identified

```bash
cat >> .logs/risks.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') SECURITY — No rate limiting on auth endpoints
- **Specialist**: Security Engineer
- **Summary**: Login and registration endpoints have no rate limiting
- **Probability**: high
- **Mitigation**: Add express-rate-limit, 10 attempts per 15 min on login
- **Status**: open
- **Impact**: high
---
LOGEOF
```

## Example: Scope Change

```bash
cat >> .logs/corrections.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') SCOPE_CHANGE — Added OAuth login
- **Specialist**: PM
- **Summary**: User requested Google OAuth in addition to email/password
- **Original plan**: Email/password only
- **New plan**: Email/password + Google OAuth
- **Effort impact**: +3 tasks (~2 hours)
- **Status**: resolved
- **Impact**: medium
---
LOGEOF
```

## Example: Handoff

```bash
cat >> .logs/communications.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') HANDOFF — Backend Dev → Frontend Dev
- **Specialist**: Orchestrator
- **Summary**: REST API for /api/users is complete (CRUD + auth). Frontend needs login page, dashboard, and profile page.
- **Status**: resolved
- **Impact**: low
---
LOGEOF
```

## Example: Inter-Skill Communication

```bash
cat >> .logs/communications.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') INTER_SKILL — Tech Lead ↔ Security Engineer
- **Specialist**: Tech Lead
- **Summary**: Asked Security Engineer to review JWT implementation. Recommendation: switch from HS256 to RS256, add refresh token rotation.
- **From**: Tech Lead
- **To**: Security Engineer
- **Decision/Outcome**: Switching to RS256 with 15min access + 7day refresh tokens
- **Status**: resolved
- **Impact**: high
---
LOGEOF
```

## Example: Metrics Snapshot

```bash
# Count metrics from logs and write snapshot
completed=$(grep -c "COMPLETED" .logs/activity.md 2>/dev/null || echo 0)
bugs=$(grep -c "BUG" .logs/issues.md 2>/dev/null || echo 0)
open=$(grep -c "Status.*open" .logs/issues.md 2>/dev/null || echo 0)
risks=$(grep -c "Status.*open" .logs/risks.md 2>/dev/null || echo 0)
changes=$(grep -c "SCOPE_CHANGE" .logs/corrections.md 2>/dev/null || echo 0)

cat >> .logs/metrics.md << LOGEOF

### $(date '+%Y-%m-%d %H:%M') SPRINT_SNAPSHOT — Batch 1 Complete
- **Planned tasks**: 6
- **Completed**: $completed
- **Bugs found**: $bugs
- **Open issues**: $open
- **Open risks**: $risks
- **Scope changes**: $changes
---
LOGEOF
```

## Example: Generate Report

```bash
python3 skills/project-monitor/templates/generate-report.py
```

## Quick Query Commands

```bash
# What's open?
grep -B1 "Status.*open" .logs/issues.md

# Recent activity
tail -20 .logs/activity.md

# How many bugs?
grep -c "BUG" .logs/issues.md

# What did we decide?
grep "^### " .logs/decisions.md

# Any scope changes?
grep "^### " .logs/corrections.md

# Last session summary
grep -A10 "SESSION_END" .logs/sessions.md | tail -12
```

## Example: Skill Added or Modified

```bash
cat >> .logs/activity.md << 'LOGEOF'

### $(date '+%Y-%m-%d %H:%M') SKILL_CHANGE — Added Creative Intelligence skill
- **Specialist**: Orchestrator
- **Summary**: New skill added: Creative Intelligence Suite (brainstorming, design thinking, storytelling, innovation). Routing table, CLAUDE.md, and QUICKSTART.md updated.
- **Change type**: added
- **Files affected**: skills/creative-intelligence/SKILL.md
- **Status**: resolved
- **Impact**: medium
---
LOGEOF
```
