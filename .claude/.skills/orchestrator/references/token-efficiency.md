# Token Efficiency Guide

## Why This Matters
Claude Code charges by tokens. Every word in context costs money. This guide keeps your daily usage lean.

## The 5 Rules

### 1. Load Lazy, Not Eager
```
❌ Read all 12 skill files at session start
✅ Read orchestrator → read ONE specialist when needed
```
Savings: ~20K tokens per session

### 2. Summarize, Don't Repeat
When switching between specialists:
```
❌ "Here's everything we discussed for the last 30 minutes..."
✅ "HANDOFF: Auth API done (JWT + refresh tokens). Need: frontend login form."
```
Savings: ~2-5K tokens per handoff

### 3. Batch Smart
```
❌ 20 tiny tasks with full context reload each time
✅ 3-4 batches of related tasks, one checkpoint per batch
```
Savings: ~10K+ tokens per session

### 4. Confirm Before Generating
```
❌ Generate 200 lines of code, user says "wrong approach", redo
✅ "I'll use Strategy Pattern here. Good?" → user confirms → generate
```
Savings: prevents full rewrites

### 5. Reference Files Are Optional
The `references/` folders contain deep dives. Only read them when:
- User asks for something specific (e.g., "harden this Dockerfile")
- You genuinely need the detail to do the task correctly
- The main SKILL.md doesn't cover the edge case

```
❌ Read all 5 DevSecOps reference files for a simple pipeline setup
✅ Read only cicd-security.md when setting up GitHub Actions
```

## Daily Token Budget Targets

| Session Type | Expected Tokens | Skill Loads |
|---|---|---|
| Quick bug fix | 3-8K | Orchestrator + 1 dev + tester |
| Small feature | 8-15K | Orchestrator + tech lead + 1-2 devs + tester |
| Large feature | 15-30K | Multiple specialists across batches |
| New project scaffold | 15-25K | PM + tech lead + DBA + devs + devops |
| Database design | 5-10K | DBA + backend dev |
| UX/UI design | 5-12K | UX + UI + frontend + copywriter |
| Security audit | 5-15K | Security eng + DevSecOps + 1-2 reference files |
| Brainstorm session | 3-8K | Creative Intelligence + PM or Tech Lead |
| Test strategy | 5-10K | Test Architect + Tester + Security Engineer |
| Document-first build | 10-20K | PM (PRD) + Tech Lead (arch) + Scrum (stories) + devs |
| Content creation | 5-10K | Marketer + copywriter or content |
| Sprint planning | 3-8K | PM + scrum master |

## Anti-Patterns to Avoid

| Anti-Pattern | Why It's Wasteful | Do Instead |
|---|---|---|
| Explaining what you're about to do in 10 lines | Tokens spent on narration | 1-2 line summary |
| Re-reading files you already read this session | Redundant context | Trust your memory within session |
| Generating all code then reviewing all at once | Large diff = large tokens | Generate + review per task |
| Loading marketing skills for a backend bug | Wrong specialist | Follow the routing table |
| Writing full test suites for throwaway prototypes | Over-engineering | Ask user: "tests needed here?" |
| Making separate tool calls just for logging | Double the tool calls | Append log in same bash command |
| Reading all 8 log files on session start | Massive context load | Read ONLY last SESSION_END entry |
| Logging trivial actions (typo fix, formatting) | Log noise | Only log decisions, completions, issues |
