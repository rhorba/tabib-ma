#!/usr/bin/env python3
"""
Project Status Report Generator
Reads .logs/ directory and produces a structured status report.

Usage:
    python skills/project-monitor/templates/generate-report.py
    python skills/project-monitor/templates/generate-report.py --format md > report.md
    python skills/project-monitor/templates/generate-report.py --format html > report.html
"""

import os
import re
import sys
from datetime import datetime
from collections import Counter

LOGS_DIR = ".logs"

def count_entries(filepath, pattern=None):
    """Count log entries (### headers) in a file, optionally filtered."""
    if not os.path.exists(filepath):
        return 0
    with open(filepath, 'r') as f:
        content = f.read()
    entries = re.findall(r'^### .+', content, re.MULTILINE)
    if pattern:
        entries = [e for e in entries if pattern.lower() in e.lower()]
    return len(entries)

def count_by_status(filepath, status):
    """Count entries with a specific status."""
    if not os.path.exists(filepath):
        return 0
    with open(filepath, 'r') as f:
        content = f.read()
    return len(re.findall(rf'Status.*{status}', content, re.IGNORECASE))

def count_by_impact(filepath, impact):
    """Count entries with a specific impact level."""
    if not os.path.exists(filepath):
        return 0
    with open(filepath, 'r') as f:
        content = f.read()
    return len(re.findall(rf'Impact.*{impact}', content, re.IGNORECASE))

def get_last_session(filepath):
    """Get the last SESSION_END entry."""
    if not os.path.exists(filepath):
        return None
    with open(filepath, 'r') as f:
        content = f.read()
    sessions = re.split(r'(?=^### )', content, flags=re.MULTILINE)
    end_sessions = [s for s in sessions if 'SESSION_END' in s]
    return end_sessions[-1].strip() if end_sessions else None

def get_recent_entries(filepath, n=5):
    """Get the last N entries from a log file."""
    if not os.path.exists(filepath):
        return []
    with open(filepath, 'r') as f:
        content = f.read()
    entries = re.split(r'(?=^### )', content, flags=re.MULTILINE)
    entries = [e.strip() for e in entries if e.strip() and e.strip().startswith('###')]
    return entries[-n:]

def get_open_items(filepath):
    """Get entries with status: open or in-progress."""
    if not os.path.exists(filepath):
        return []
    with open(filepath, 'r') as f:
        content = f.read()
    entries = re.split(r'(?=^### )', content, flags=re.MULTILINE)
    open_items = []
    for entry in entries:
        if re.search(r'Status.*(?:open|in-progress)', entry, re.IGNORECASE):
            title = re.search(r'^### .+', entry, re.MULTILINE)
            impact = re.search(r'Impact.*(\w+)', entry, re.IGNORECASE)
            if title:
                open_items.append({
                    'title': title.group().replace('### ', ''),
                    'impact': impact.group(1) if impact else 'unknown'
                })
    return open_items

def calculate_health_score():
    """Calculate project health score (0-100)."""
    score = 100
    issues_file = os.path.join(LOGS_DIR, "issues.md")
    risks_file = os.path.join(LOGS_DIR, "risks.md")
    activity_file = os.path.join(LOGS_DIR, "activity.md")

    # Deduct for open critical issues
    score -= count_by_impact(issues_file, "critical") * 20
    # Deduct for open high issues
    open_high = 0
    if os.path.exists(issues_file):
        with open(issues_file, 'r') as f:
            content = f.read()
        entries = re.split(r'(?=^### )', content, flags=re.MULTILINE)
        for entry in entries:
            if (re.search(r'Status.*open', entry, re.IGNORECASE) and
                re.search(r'Impact.*high', entry, re.IGNORECASE)):
                open_high += 1
    score -= open_high * 10

    # Deduct for open blockers
    score -= count_entries(issues_file, "BLOCKER") * 15

    # Deduct for open critical risks
    critical_risks = 0
    if os.path.exists(risks_file):
        with open(risks_file, 'r') as f:
            content = f.read()
        entries = re.split(r'(?=^### )', content, flags=re.MULTILINE)
        for entry in entries:
            if (re.search(r'Status.*open', entry, re.IGNORECASE) and
                re.search(r'Impact.*critical', entry, re.IGNORECASE)):
                critical_risks += 1
    score -= critical_risks * 10

    return max(0, min(100, score))

def generate_report(fmt="md"):
    """Generate the full status report."""
    now = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    # Gather metrics
    total_activities = count_entries(os.path.join(LOGS_DIR, "activity.md"))
    completed = count_entries(os.path.join(LOGS_DIR, "activity.md"), "COMPLETED")
    milestones = count_entries(os.path.join(LOGS_DIR, "activity.md"), "MILESTONE")
    
    total_issues = count_entries(os.path.join(LOGS_DIR, "issues.md"))
    open_issues = count_by_status(os.path.join(LOGS_DIR, "issues.md"), "open")
    resolved_issues = count_by_status(os.path.join(LOGS_DIR, "issues.md"), "resolved")
    bugs = count_entries(os.path.join(LOGS_DIR, "issues.md"), "BUG")
    blockers = count_entries(os.path.join(LOGS_DIR, "issues.md"), "BLOCKER")
    
    total_decisions = count_entries(os.path.join(LOGS_DIR, "decisions.md"))
    total_risks = count_entries(os.path.join(LOGS_DIR, "risks.md"))
    open_risks = count_by_status(os.path.join(LOGS_DIR, "risks.md"), "open")
    scope_changes = count_entries(os.path.join(LOGS_DIR, "corrections.md"))
    sessions = count_entries(os.path.join(LOGS_DIR, "sessions.md"), "SESSION_END")
    
    health = calculate_health_score()
    health_emoji = "🟢" if health >= 90 else ("🟡" if health >= 70 else "🔴")
    health_label = "On Track" if health >= 90 else ("At Risk" if health >= 70 else "Needs Attention")
    
    bug_rate = f"{bugs/completed:.2f}" if completed > 0 else "N/A"
    fix_rate = f"{resolved_issues/total_issues*100:.0f}%" if total_issues > 0 else "N/A"
    
    open_items = get_open_items(os.path.join(LOGS_DIR, "issues.md"))
    open_risk_items = get_open_items(os.path.join(LOGS_DIR, "risks.md"))
    recent_decisions = get_recent_entries(os.path.join(LOGS_DIR, "decisions.md"), 5)
    recent_corrections = get_recent_entries(os.path.join(LOGS_DIR, "corrections.md"), 3)
    last_session = get_last_session(os.path.join(LOGS_DIR, "sessions.md"))
    
    report = f"""# Project Status Report — {now}

## Health: {health_emoji} {health_label} ({health}/100)

## Progress Summary
| Metric | Value |
|---|---|
| Tasks completed | {completed} |
| Milestones reached | {milestones} |
| Sessions | {sessions} |
| Decisions made | {total_decisions} |

## Quality Metrics
| KPI | Value |
|---|---|
| Total issues | {total_issues} |
| Open issues | {open_issues} |
| Resolved issues | {resolved_issues} |
| Bugs found | {bugs} |
| Blockers | {blockers} |
| Bug rate | {bug_rate} per task |
| Fix rate | {fix_rate} |

## Risk & Change Metrics
| KPI | Value |
|---|---|
| Open risks | {open_risks} / {total_risks} |
| Scope changes | {scope_changes} |

## Open Issues"""

    if open_items:
        for item in open_items:
            icon = "🔴" if item['impact'] in ('critical', 'high') else "🟡"
            report += f"\n- {icon} [{item['impact'].upper()}] {item['title']}"
    else:
        report += "\n- None 🎉"

    report += "\n\n## Open Risks"
    if open_risk_items:
        for item in open_risk_items:
            report += f"\n- ⚠️ [{item['impact'].upper()}] {item['title']}"
    else:
        report += "\n- None"

    report += "\n\n## Recent Decisions"
    if recent_decisions:
        for d in recent_decisions[-3:]:
            title = re.search(r'^### (.+)', d, re.MULTILINE)
            if title:
                report += f"\n- {title.group(1)}"
    else:
        report += "\n- None yet"

    if recent_corrections:
        report += "\n\n## Recent Plan Changes"
        for c in recent_corrections:
            title = re.search(r'^### (.+)', c, re.MULTILINE)
            if title:
                report += f"\n- {title.group(1)}"

    if last_session:
        report += f"\n\n## Last Session\n```\n{last_session}\n```"

    report += "\n"
    return report


if __name__ == "__main__":
    if not os.path.exists(LOGS_DIR):
        print(f"Error: {LOGS_DIR}/ directory not found. Run init-logs.sh first.")
        sys.exit(1)
    
    fmt = "md"
    if "--format" in sys.argv:
        idx = sys.argv.index("--format")
        if idx + 1 < len(sys.argv):
            fmt = sys.argv[idx + 1]
    
    print(generate_report(fmt))
