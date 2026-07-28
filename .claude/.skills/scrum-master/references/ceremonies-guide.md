# Scrum Master — Sprint Ceremonies Reference

Load when facilitating sprint planning, retrospectives, or backlog refinement.

---

## Ceremonies at a Glance (2-week sprint)

| Ceremony | When | Duration | Output |
|---|---|---|---|
| Sprint Planning | Sprint start | 2-4 hours | Sprint goal + committed backlog |
| Daily Standup | Every day | 15 min | Blockers surfaced |
| Backlog Refinement | Mid-sprint | 1-2 hours | Groomed + estimated stories |
| Sprint Review | Sprint end | 1-2 hours | Demo + stakeholder feedback |
| Retrospective | After review | 1-1.5 hours | ≥1 actionable improvement |

---

## Sprint Planning Agenda

```
[0-15 min]   PO presents sprint goal + top backlog items
[15-45 min]  Team clarifies acceptance criteria
[45-90 min]  Team estimates and confirms: "Can we deliver this goal?"
[90-120 min] Break stories into tasks (< 1 day each)
[Last 5 min] Confirm sprint goal in one sentence
```

**Sprint goal formula**: "By the end of this sprint, users can [X] so that [outcome Y]."

---

## Story Readiness Checklist

A story is "ready" for planning when:
- [ ] Acceptance criteria in Given/When/Then or checklist format
- [ ] Dependencies identified and resolved or mitigated
- [ ] Estimated (story points)
- [ ] UI mockup attached (if UI work)
- [ ] API contract defined (if backend work)
- [ ] No open clarifying questions

**Target**: 2 sprints of ready stories in the backlog at all times.

---

## Daily Standup (3 questions, 15 min)

1. What did I complete since last standup?
2. What will I complete by next standup?
3. What is blocking me?

**Anti-patterns to prevent:**
- Status report to the manager (standup is for the team)
- Problem-solving during standup → park it: "Let's discuss after"
- Board not updated before standup

---

## Sprint Review Format

```
[0-5 min]   PO: sprint goal recap
[5-30 min]  Team demos working software against acceptance criteria
[30-50 min] Stakeholder feedback → PO captures to backlog
[50-60 min] Velocity recorded: [points done / committed]
```

**Definition of Done** — only demo items that meet all criteria:
- [ ] Code reviewed and merged
- [ ] Tests passing (≥ 80% coverage gate)
- [ ] Security review done (if touching auth / payments / data)
- [ ] Deployed to staging

---

## Retrospective Formats

### Start / Stop / Continue (classic)
```
Start:    What should we begin doing?
Stop:     What should we stop doing?
Continue: What's working and should continue?
```

### 4Ls
```
Liked / Learned / Lacked / Longed for
```

### Sailboat
```
Wind (pushing us forward) / Anchors (slowing us) / Rocks (risks ahead)
```

**Facilitation rules:**
1. Prime directive: assume everyone did their best
2. One improvement per retro — more than 3 action items gets nothing done
3. Every action item has an owner + due date ("team" is not an owner)
4. Open previous retro's items first — did we follow through?
5. Silent writing before discussion — prevents groupthink

---

## Story Point Scale

| Points | Complexity | Example |
|---|---|---|
| 1 | Trivial | Text copy change |
| 2 | Small | Add a form field |
| 3 | Medium | New API endpoint |
| 5 | Medium | Feature with DB work |
| 8 | Large | Complex integration |
| 13 | X-Large | Spike or split |

Stories > 8 must be split before sprint planning.
Spikes = time-boxed research tasks for unknowns.

---

## Velocity & Capacity

```
Velocity = story points completed / sprint (3-sprint rolling average)

Capacity:
  Total working days − holidays − PTO − ceremony time
  Sprint commitment ≈ velocity ± 20% adjusted for capacity
```
