#!/bin/bash
# Initialize project log directory
# Run: bash skills/project-monitor/templates/init-logs.sh

LOG_DIR=".logs"

mkdir -p "$LOG_DIR"

# Create log files with headers
cat > "$LOG_DIR/activity.md" << 'EOF'
# Activity Log
<!-- Tracks completed tasks, milestones, and deliverables -->
<!-- Format: ### [YYYY-MM-DD HH:MM] COMPLETED/MILESTONE — Title -->

EOF

cat > "$LOG_DIR/decisions.md" << 'EOF'
# Decision Log
<!-- Tracks architecture decisions, approach selections, tool choices -->
<!-- Format: ### [YYYY-MM-DD HH:MM] ARCHITECTURE/APPROACH/TOOL — Title -->

EOF

cat > "$LOG_DIR/issues.md" << 'EOF'
# Issue Log
<!-- Tracks bugs, errors, blockers with status -->
<!-- Format: ### [YYYY-MM-DD HH:MM] BUG/BLOCKER/ERROR — Title -->

EOF

cat > "$LOG_DIR/risks.md" << 'EOF'
# Risk Log
<!-- Tracks risks identified and their mitigations -->
<!-- Format: ### [YYYY-MM-DD HH:MM] SECURITY/PERFORMANCE/DEPENDENCY — Title -->

EOF

cat > "$LOG_DIR/corrections.md" << 'EOF'
# Corrections & Plan Changes
<!-- Tracks scope changes, pivots, plan adjustments -->
<!-- Format: ### [YYYY-MM-DD HH:MM] SCOPE_CHANGE/PIVOT/REPLAN — Title -->

EOF

cat > "$LOG_DIR/communications.md" << 'EOF'
# Communications Log
<!-- Tracks key questions, user preferences, handoffs -->
<!-- Format: ### [YYYY-MM-DD HH:MM] PREFERENCE/HANDOFF/QUESTION — Title -->

EOF

cat > "$LOG_DIR/sessions.md" << 'EOF'
# Session Log
<!-- Tracks session starts and ends for resumption -->
<!-- Format: ### [YYYY-MM-DD HH:MM] SESSION_START/SESSION_END -->

EOF

cat > "$LOG_DIR/metrics.md" << 'EOF'
# Project Metrics
<!-- KPI snapshots over time -->
<!-- Format: ### [YYYY-MM-DD HH:MM] SPRINT_SNAPSHOT/DAILY_SNAPSHOT — Title -->

EOF

# Add to .gitignore if not already there
if [ -f ".gitignore" ]; then
  grep -q "^\.logs/" .gitignore || echo -e "\n# Project monitoring logs\n.logs/" >> .gitignore
fi

echo "✅ Log directory initialized at $LOG_DIR"
echo "   8 log files created: activity, decisions, issues, risks, corrections, communications, sessions, metrics"
