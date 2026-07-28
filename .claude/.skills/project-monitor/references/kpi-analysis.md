# KPI Analysis & Reporting Reference

## Reading Logs for Reports

**Token-efficient log reading**: Never `cat` entire log files. Use targeted reads:

```bash
# Count entries by type
grep -c "^### " .logs/activity.md        # total activities
grep -c "BUG" .logs/issues.md            # total bugs
grep -c "BLOCKER" .logs/issues.md        # total blockers
grep -c "Status.*open" .logs/issues.md   # open issues

# Get last N entries from any log
tail -n 30 .logs/activity.md             # last ~3-4 entries

# Find unresolved items
grep -B1 -A4 "Status.*open" .logs/issues.md
grep -B1 -A4 "Status.*in-progress" .logs/issues.md

# Count by severity
grep -c "Impact.*critical" .logs/issues.md
grep -c "Impact.*high" .logs/issues.md

# Session history
grep -A6 "SESSION_END" .logs/sessions.md | tail -20
```

## KPI Calculation Scripts

### Velocity
```bash
# Tasks completed in current sprint/session
completed=$(grep -c "COMPLETED" .logs/activity.md)
echo "Velocity: $completed tasks"
```

### Bug Rate
```bash
bugs=$(grep -c "BUG" .logs/issues.md)
tasks=$(grep -c "COMPLETED" .logs/activity.md)
echo "Bug rate: $bugs bugs / $tasks tasks = $(echo "scale=2; $bugs/$tasks" | bc)"
```

### Scope Creep
```bash
changes=$(grep -c "SCOPE_CHANGE" .logs/corrections.md)
echo "Scope changes: $changes"
```

### Open vs Closed Issues
```bash
total=$(grep -c "^### " .logs/issues.md)
open=$(grep -c "Status.*open" .logs/issues.md)
resolved=$(grep -c "Status.*resolved" .logs/issues.md)
echo "Issues: $total total, $open open, $resolved resolved"
```

## Health Score Calculation

```
Score starts at 100, deduct for problems:

- Open critical issue:     -20 each
- Open high issue:         -10 each
- Open blocker:            -15 each
- Completion rate < 80%:   -10
- Scope creep > 20%:       -10
- Open critical risk:      -10 each
- Estimation off by > 50%: -5

Health:
  90-100 → 🟢 On Track
  70-89  → 🟡 At Risk
  < 70   → 🔴 Blocked/Troubled
```

## Report Types

### Daily Standup (quick — read last session + open issues)
```
What was done:    [last 3-5 activities]
What's next:      [current batch, next task]
Blockers:         [open blockers]
```

### Sprint Report (full — read all logs for sprint period)
```
Velocity, completion rate, bug rate, scope changes,
decisions made, risks managed, key achievements
```

### Retrospective (analytical — patterns across sprints)
```
Compare metrics across sprints, identify recurring issues,
measure improvement on previous action items
```

### Stakeholder Report (high-level — for non-technical audience)
```
Progress %, key milestones, risks, timeline status
No technical details, focus on outcomes
```

## Trend Analysis

When you have 3+ metric snapshots, look for:

| Pattern | Signal | Action |
|---|---|---|
| Velocity declining | Team is slowing down | Check for tech debt, blockers, unclear requirements |
| Bug rate increasing | Quality dropping | Add more testing, review code standards |
| Scope creep rising | Requirements unstable | Tighter scope definition, change management |
| Estimation improving | Team is calibrating | Keep current process |
| Blockers recurring | Systemic issue | Address root cause, not symptoms |
| Rework rate high | Miscommunication | Better specs, more upfront design |
