# UX Designer — UX Patterns & Usability Reference

Load when designing user flows, running usability reviews, or evaluating interaction patterns.

---

## Nielsen's 10 Usability Heuristics

| # | Heuristic | Violation Example | Fix |
|---|---|---|---|
| 1 | Visibility of system status | No loading indicator | Add spinner / progress |
| 2 | Match system and real world | "Execute query" for non-tech users | "Search" |
| 3 | User control and freedom | No undo after deletion | Add undo / confirmation |
| 4 | Consistency and standards | Different button styles per page | Design system |
| 5 | Error prevention | No confirmation before delete | Confirm dialog |
| 6 | Recognition over recall | Blank search field, no suggestions | Autocomplete / history |
| 7 | Flexibility and efficiency | No keyboard shortcuts for power users | Add shortcuts |
| 8 | Aesthetic and minimalist design | 15 fields in one form | Progressive disclosure |
| 9 | Help users recognize, diagnose, recover | "Error 500" with no guidance | "Try again / contact support" |
| 10 | Help and documentation | No in-app guidance | Onboarding tour / tooltips |

---

## Common UX Patterns

### Progressive Disclosure
Show only what the user needs now. Reveal complexity on demand.
```
Basic form → [ Advanced options ▾ ] → reveals additional fields on click
```

### Empty States
Every list / table needs an empty state — don't show a blank screen.
```
[Illustration]
No orders yet.
[Create your first order →]
```

### Skeleton Screens vs. Spinners
- Skeleton: use for content-heavy layouts (dashboards, feeds) — reduces perceived load time
- Spinner: use for short, indeterminate operations (< 3 seconds)

### Inline Validation
Validate on blur (not on every keystroke). Show error where the field is, not at top.
```
Email: [alice@            ]  ← after leaving field
       ↑ "Please enter a valid email address"
```

### Destructive Action Pattern
```
1. Disabled button until user types confirmation text, or
2. Two-step confirmation: "Delete" → "Are you sure? This cannot be undone." → "Yes, delete"
Never: single click to permanently delete with no undo
```

---

## User Flow Template

```markdown
## Flow: [Name] — e.g., "Password Reset"

**Entry point**: [Where the user starts — e.g., login page "Forgot password" link]
**Exit point**: [Where they end — e.g., logged into dashboard]
**Happy path**:
  1. User clicks "Forgot password"
  2. Enters email address → submits
  3. System sends reset email
  4. User opens email, clicks link
  5. User enters new password (twice)
  6. Success → redirected to login

**Error paths**:
  - Email not found → show generic message (don't leak existence)
  - Link expired → offer to resend
  - Passwords don't match → inline error

**Edge cases**:
  - User clicks link twice → second use invalidated, graceful error
  - User is on mobile → email link opens in mobile browser, not desktop session
```

---

## Wireframe Annotation Conventions

```
[ ] — checkbox or toggle
( ) — radio button
[___] — text input field
[Button] — primary action (filled)
[Button] — secondary action (outlined)
[x] — close / dismiss
▾ — dropdown
↑↓ — sortable column
? — tooltip trigger
* — required field marker
```

---

## Usability Review Checklist

### Navigation
- [ ] User always knows where they are (breadcrumbs, active nav item)
- [ ] User can always get back (back button works, no dead ends)
- [ ] Primary actions are discoverable (not hidden in menus)

### Forms
- [ ] Labels above inputs (not placeholder-only)
- [ ] Related fields grouped
- [ ] Clear primary action (one dominant CTA per view)
- [ ] Validation errors explain what went wrong and how to fix it

### Mobile
- [ ] Touch targets ≥ 44×44px (Apple HIG) / 48×48dp (Material)
- [ ] No hover-only interactions
- [ ] Content readable without zooming (no tiny fonts)
- [ ] Forms don't zoom on focus (font-size ≥ 16px on inputs)

### Copy
- [ ] Button text says what happens: "Save changes" not "OK"
- [ ] Error messages in plain language: "Enter a password with at least 8 characters"
- [ ] No unexplained jargon

---

## User Research Methods Quick Reference

| Method | When | Output |
|---|---|---|
| User interview | Early discovery | Mental models, pain points |
| Card sorting | IA design | Category groupings |
| Tree testing | IA validation | Navigation success rate |
| Usability test (5 users) | Prototype validation | Task success, confusion points |
| A/B test | Post-launch optimization | Conversion lift |
| Analytics review | Post-launch | Drop-off points, popular paths |

"First test with 5 users — they'll surface 80% of the critical issues."
