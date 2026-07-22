# UI: Tabib.ma
**References**: docs/ux-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UI Designer | **Status**: Draft

## 1. Foundation Decision
**Using an existing component library**: Tailwind CSS + shadcn/ui (Radix UI primitives) on React. Radix gives WCAG-compliant keyboard/focus/ARIA behavior out of the box (satisfies PRD NFR-6 and the UX doc's accessibility constraints) without building accessible components from scratch. Tailwind's logical properties (`ps-`/`pe-`/`start-`/`end-` instead of `pl-`/`pr-`/`left-`/`right-`) are used throughout specifically to support RTL (Arabic) mirroring without a separate RTL stylesheet.

## 2. Design Tokens

### Colors
Healthcare product — calm, trustworthy, not clinical-cold. Two brand colors + standard semantic set:
```
Primary:      #0F766E  (teal — calm, medical-adjacent without feeling sterile) — CTAs, links, active nav
Primary-dark: #0C5F58  (hover/active states)
Secondary:    #F59E0B  (warm amber — used sparingly for "attention" secondary actions, e.g. "join video now")
Background:   #FFFFFF (light) / #0F1417 (dark — Phase 2, tokens defined now to avoid rework later)
Surface:      #F4F7F6 (light) / #1A2226 (dark)
Text:         #111827 (light) / #F0F0F0 (dark)
Text-muted:   #6B7280 (light) / #9AA5AC (dark)
Border:       #E2E8E6 (light) / #2C363B (dark)
Success:      #16A34A  — booking confirmed, payment succeeded
Warning:      #F59E0B  — slot expiring soon, poor connection notice
Error:        #DC2626  — payment failed, validation errors
Info:         #2563EB  — informational banners (e.g., "audio-only mode suggested")
```
All text/background pairs verified at 4.5:1 contrast minimum (WCAG AA). Status is never conveyed by color alone — every semantic color pairs with an icon or text label (e.g., booking status shows both a colored dot AND the word "Confirmed").

**Dark mode**: tokens defined above but **not built for v1** (YAGNI — no user demand signal yet for a healthcare booking app used in short task-focused sessions). Defining the tokens now costs nothing and avoids a full re-theme later if requested.

### Typography
```
Font family: Inter (excellent Latin + reasonable Arabic support via Inter's extended coverage;
             fall back to system Arabic-first font — e.g., "Noto Sans Arabic" — when locale=ar)
Monospace:   not needed for this product (no code/data display use case)

Scale (1.25 ratio):
  xs:   12px  — timestamps, metadata
  sm:   14px  — secondary text, form help text
  base: 16px  — body (never smaller than 14px on mobile per UX mobile rule)
  lg:   20px  — sub-headings, doctor name in search results
  xl:   24px  — section headings (dashboard sections)
  2xl:  30px  — page headings
  3xl:  36px  — used once: booking-confirmed success screen

Weights: 400 body, 500 emphasis, 600 headings, 700 reserved for critical alerts only
Line height: 1.5 body, 1.3 headings (1.6 body recommended for Arabic script — slightly taller line-height improves legibility for Arabic ligatures)
```

### Spacing
Standard 4px base grid (per UI Designer default) — 1/2/3/4/6/8/12/16 scale, used consistently across both LTR and RTL layouts since it's direction-agnostic.

### Border Radius
```
sm:   4px   — badges (status pills: Confirmed/Pending/Cancelled), form inputs
md:   8px   — cards (doctor cards, appointment cards) — default
lg:   12px  — modals, the booking confirmation panel
full: 9999px — avatars, doctor profile photos
```

## 3. Component Patterns (product-specific)

### Doctor Card (search results)
```
Content order: Photo → Name + Specialty → Rating (stars + count) → Fee (MAD) → Next available slot → [View Profile] CTA
States: default, hover (slight elevation), loading (skeleton: gray blocks matching final layout)
Verification badge: small checkmark icon next to name if verification_status=APPROVED — never shown as a raw "PENDING" state to patients (unverified doctors don't appear in search at all, per Architecture module design)
```

### Appointment Status Badge
```
Pending Payment → amber pill, "Awaiting Payment"
Confirmed       → teal pill, "Confirmed"
Completed       → gray pill, "Completed"
Cancelled       → red-outline pill, "Cancelled"
No-Show         → red-outline pill, "No-Show"
Always icon + text, never color-only (Section 2 rule)
```

### Booking CTA Button
```
Primary button (filled, Primary color), 48px height (lg size — this is the highest-stakes action in the product, sized for confidence and thumb-friendly mobile tapping)
States: default, hover, active, focus (visible ring — Radix default), disabled (during slot re-validation), loading (spinner replaces label text, button stays same width to avoid layout shift)
```

### Video Consultation Controls
```
Bottom-anchored control bar (mobile: thumb zone, per UX/UI mobile rules): Mute | End Call | Switch to Audio-only | (Doctor only: Issue Prescription)
Touch targets: 44x44px minimum (mobile rule)
All controls keyboard-operable with visible focus ring + aria-label (UX doc accessibility requirement)
```

### Forms (booking details, doctor onboarding, prescription entry)
```
Labels always visible (no placeholder-only labels — UI Designer default rule)
Error state: red border + inline message below field + icon
Required fields are the default (unmarked); optional fields explicitly labeled "(optional)" per UI Designer rule
RTL: field order mirrors (label/input alignment flips to right-anchored in Arabic) — Tailwind logical properties handle this automatically if authored correctly
```

## 4. Layout Grid
Standard responsive grid (12/8/4 columns desktop/tablet/mobile per UI Designer default), max content width 1280px. Clinic Admin and Platform Admin dashboards get a wider max-width allowance (1440px) since they're data-dense, desktop-primary surfaces (per UX personas — Fatima and Platform Admin are desktop power users).

## 5. RTL (Arabic) Implementation Rules
- Use Tailwind logical properties exclusively (`ms-`/`me-`, `text-start`/`text-end`) — never `ml-`/`mr-`/`text-left`/`text-right` in shared components.
- Icons with inherent direction (back chevron, arrow CTAs) must flip automatically via `rtl:` variant or an icon component that accepts a `dir`-aware prop — flagged explicitly for Frontend Dev, this is the single most common RTL bug source.
- Numbers (fees in MAD, dates, times) remain LTR even within RTL Arabic text blocks — verify this doesn't visually break mid-sentence (standard Arabic typographic convention, `dir="ltr"` span wrapping for numeric fragments if needed).
- Test the entire booking flow (UX Flow 1) end-to-end in Arabic/RTL before Sprint 1 docs are considered "implementation-ready" — added as a Test Architect acceptance scenario.

## 6. Responsive Rules (product-specific application)
- Mobile-first for all Patient-facing screens (search, book, video) — Patient persona (Amina) is primarily mobile per UX persona.
- Desktop-first acceptable for Clinic Admin/Platform Admin dashboards (data tables) — still must degrade to card layout on mobile per UI Designer table rule, but not the primary design target.
- Video room: mobile layout stacks controls below video; desktop layout can show controls overlaid or in a side panel.

## 7. Component Handoff Spec (example — Booking CTA, pattern to replicate for every component)
```markdown
## Component: BookingConfirmButton
- Design tokens used: Primary color, lg button size (48px), md border-radius
- States: default, hover, active, focus (Radix default ring), disabled (during re-validation), loading
- Responsive behavior: full-width on mobile, auto-width on desktop
- Animations: 150ms ease-out on hover elevation; spinner fade-in at 100ms if load > 300ms (avoid flash for fast responses)
- Accessibility: aria-busy during loading, aria-disabled synced with visual disabled state, focus ring visible in both LTR/RTL
```

## 8. Handoff Points
- **→ Frontend Dev**: Tokens (Section 2), component patterns (Section 3), RTL implementation rules (Section 5) — the RTL rules are the highest-risk-of-being-skipped item, call this out explicitly in Sprint 2 planning.
- **→ Copywriter**: French/Arabic string requirements per UX IA — status labels, error messages, and CTA copy need both locales from day one, not retrofitted.
- **→ Test Architect**: RTL/Arabic full-flow test (Section 5) and contrast-ratio verification (Section 2) as acceptance criteria.
