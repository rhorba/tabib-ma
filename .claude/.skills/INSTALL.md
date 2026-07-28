# Claude Team Skills — Installation Guide

## Quick Setup (Claude Code)

### Step 1: Copy the skills folder
Place the entire `skills/` directory in your project root:

```
your-project/
├── skills/
│   ├── orchestrator/
│   ├── project-manager/
│   ├── scrum-master/
│   ├── tech-lead/
│   ├── security-engineer/
│   ├── dba/
│   ├── ux-designer/
│   ├── ui-designer/
│   ├── backend-dev/
│   ├── frontend-dev/
│   ├── tester/
│   ├── deployment/
│   ├── devops-devsecops/
│   │   └── references/        ← 5 deep-dive security files
│   ├── digital-marketer/
│   ├── copywriter/
│   ├── content-marketer/
│   ├── creative-intelligence/   ← Brainstorming, design thinking, innovation
│   ├── test-architect/          ← Test strategy, ATDD, adversarial review
│   └── project-monitor/         ← Logging, KPIs, status reports
│       ├── references/
│       └── templates/         ← init-logs.sh script
├── .logs/                     ← Auto-created on first session
│   ├── activity.md
│   ├── decisions.md
│   ├── issues.md
│   ├── risks.md
│   ├── corrections.md
│   ├── communications.md
│   ├── sessions.md
│   └── metrics.md
├── CLAUDE.md                  ← Copy this to project root
├── QUICKSTART.md              ← Your cheat sheet
└── ... your project files
```

### Step 2: Add CLAUDE.md to project root
Copy the provided `CLAUDE.md` file to your project root. This is the entry point that tells Claude Code how to use the skills.

### Step 3: Start working
Open Claude Code and just describe what you need:
- "I need to build a user authentication feature"
- "There's a bug in the payment flow"
- "Set up CI/CD for this project"
- "Write a blog post about our new release"

The orchestrator handles the rest.

---

## How It Works

```
You say something
       │
       ▼
 CLAUDE.md loads → reads orchestrator
       │
       ▼
 Orchestrator identifies the task type
       │
       ▼
 Loads ONLY the needed specialist
       │
       ▼
 Interactive work: options → pick → execute → verify
       │
       ▼
 Switches specialist if needed (with handoff summary)
```

## Token Usage

This setup is designed to be lean:
- CLAUDE.md: ~1K tokens (always loaded)
- Orchestrator: ~2K tokens (loaded at session start)
- Each specialist: ~1-2K tokens (loaded one at a time)
- Reference files: ~3-5K tokens each (loaded only for deep dives)

**Typical session cost**: ~3-5K tokens for routing + whatever the actual coding costs.
Compare to loading everything at once: ~35K+ tokens wasted.

## Customization

### Add your project's conventions
Edit `CLAUDE.md` to include:
- Your tech stack specifics
- Naming conventions
- Deployment URLs
- Team contacts
- Any project-specific rules

### Disable skills you don't need
Remove or comment out rows in the CLAUDE.md skill locations table.
If you're purely a backend project, you can skip: frontend-dev, copywriter, content-marketer, digital-marketer.

### Add new specialists
Create a new folder in `skills/` with a `SKILL.md` following the same pattern:
1. YAML frontmatter with name + description
2. Role definition
3. Key templates/checklists
4. Handoff points to other specialists
5. Add it to the orchestrator's routing table + CLAUDE.md

---

## FAQ

**Q: Does Claude really load skills one at a time?**
A: Yes. The orchestrator tells Claude which skill to read for the current step. It never loads all 12 at once.

**Q: What if I need two specialists at the same time?**
A: The orchestrator handles this with the handoff protocol — it summarizes context in 2-3 lines when switching, so no tokens are wasted repeating full history.

**Q: Can I use these with Claude.ai (not Claude Code)?**
A: The skills are optimized for Claude Code, but you can paste individual SKILL.md content into Claude.ai conversations as needed. The orchestrator workflow works in any Claude interface.

**Q: How do I update a skill?**
A: Just edit the SKILL.md file. Changes take effect on the next session.

**Q: How does project logging work?**
A: On first session, Claude creates a `.logs/` directory with 8 structured log files (activity, decisions, issues, risks, corrections, communications, sessions, metrics). Every significant action gets appended as a log entry. You can ask for "status report", "KPIs", or "retro" at any time. Logs persist across sessions, so Claude can resume where you left off. You can also run `bash skills/project-monitor/templates/init-logs.sh` to initialize logs manually.

**Q: Should I commit `.logs/` to git?**
A: Up to you. The init script adds `.logs/` to `.gitignore` by default (keeps logs private). Remove that line if you want logs in version control for team visibility.

## Intentional Trade-offs (vs BMAD Method)

Claude Team Skills intentionally skips these BMAD features to stay **lightweight and zero-dependency**:

| BMAD Feature We Skip | Why We Skip It |
|---|---|
| NPM installer | CTS is zero-dependency — just copy markdown files, no Node.js required |
| Multi-IDE support (10+ IDEs) | CTS targets Claude Code specifically for deeper integration |
| Agent personas (named characters) | CTS uses role-based skills, not character personas — simpler, less tokens |
| Party Mode (multi-agent debate) | CTS uses sequential handoffs to save tokens (~70% less than loading all agents) |
| Modular expansion packs | CTS is a single bundle — one download, everything included |
| Scale-adaptive intelligence | CTS uses explicit YAGNI gates — user picks complexity, not auto-detected |
| Agent-as-Code (YAML compilation) | CTS uses plain markdown — editable with any text editor, no build step |
| Web bundles (ChatGPT/Gemini) | CTS is Claude Code native — optimized for one platform done well |

These are **design choices, not missing features**. CTS prioritizes simplicity, token efficiency, and operational awareness (logging, KPIs, session resumption) over platform breadth and ecosystem size.
