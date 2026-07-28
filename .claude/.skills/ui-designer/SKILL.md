---
name: ui-designer
description: >
  UI (User Interface) Designer skill for visual design, design systems, color, typography, spacing,
  and component styling. Distinct from UX Designer (which handles flows and architecture). Use when
  the user needs visual design decisions, color palettes, typography selection, spacing systems,
  design tokens, component library design, responsive layouts, dark mode, brand consistency, icon
  selection, or making things look good and polished. Trigger on: "UI design", "visual design",
  "color palette", "typography", "font", "spacing", "design system", "design tokens", "component
  library", "dark mode", "theme", "brand", "style guide", "icon", "layout grid", "make it look good",
  "polish the UI", "Figma specs", or visual/aesthetic work. NOT for user flows — that's UX Designer.
---

# UI Designer

## Role
You make things look good, feel consistent, and communicate clearly through visual design. You turn wireframes into polished interfaces.

## YAGNI UI Design
Don't build a full design system for 3 screens:
- **MVP / prototype**: Use an existing component library (shadcn, Radix, MUI). Pick 2 colors + 1 font. Done.
- **Growing product**: Define a minimal token set (colors, spacing, type scale). Customize components as needed.
- **Mature product**: Full design system with tokens, component docs, brand guidelines.

Ask: "Are we using an existing UI library?" before designing from scratch.

## Design Token System (minimal starter)

### Colors
```
Primary:     #[brand color]       — CTAs, links, active states
Primary-dark: darken 15%          — hover states
Secondary:   #[accent color]      — secondary actions
Background:  #FFFFFF / #0F0F0F    — light/dark
Surface:     #F5F5F5 / #1A1A1A   — cards, panels
Text:        #111111 / #F0F0F0   — primary text
Text-muted:  #666666 / #999999   — secondary text
Border:      #E0E0E0 / #333333   — dividers, borders
Success:     #16A34A              — confirmations
Warning:     #F59E0B              — caution
Error:       #DC2626              — errors, destructive
Info:        #2563EB              — informational
```

**Rules:**
- Max 2 brand colors + semantic colors (success/warning/error/info)
- All text must pass WCAG AA contrast (4.5:1 minimum)
- Test with colorblind simulators — never use color alone to convey meaning

### Typography
```
Font family:  1 primary (Inter, System, or brand font)
              1 monospace for code (JetBrains Mono, Fira Code)

Scale (use a ratio, e.g., 1.25 Major Third):
  xs:   12px / 0.75rem    — captions, labels
  sm:   14px / 0.875rem   — secondary text, metadata
  base: 16px / 1rem       — body text (default)
  lg:   20px / 1.25rem    — subheadings, emphasis
  xl:   24px / 1.5rem     — section headings
  2xl:  30px / 1.875rem   — page headings
  3xl:  36px / 2.25rem    — hero/display (use sparingly)

Weights: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)
Line height: 1.5 for body, 1.2 for headings
```

### Spacing
```
Use a 4px base grid:
  1:  4px     — tight padding
  2:  8px     — default inner padding
  3:  12px    — compact gaps
  4:  16px    — default gaps
  6:  24px    — section padding
  8:  32px    — large gaps
  12: 48px    — section spacing
  16: 64px    — page sections
```

**Rule:** Consistent spacing matters more than the exact values. Pick a system and stick to it.

### Border Radius
```
none: 0       — tables, inputs that need alignment
sm:   4px     — buttons, badges
md:   8px     — cards, modals (default)
lg:   12px    — larger containers
full: 9999px  — pills, avatars
```

## Component Design Patterns

### Buttons
```
Primary:    Filled, brand color — 1 per screen section (main CTA)
Secondary:  Outlined or ghost — supporting actions
Tertiary:   Text-only — low-emphasis actions
Danger:     Red filled/outlined — destructive actions (always confirm)

States: default, hover, active, focus, disabled, loading
Sizes: sm (32px h), md (40px h), lg (48px h)
```

### Forms
```
Labels: Always visible (no placeholder-only labels)
Inputs: 40-48px height, clear focus ring, error border on validation
Errors: Inline below field, red text, icon + message
Help text: Below field, muted color
Required: Mark optional fields, not required ones (most are required)
```

### Cards
```
Padding: 16-24px
Border: 1px subtle or shadow (not both)
Radius: 8px default
Hover: Slight elevation change if clickable
Content order: Image → Title → Description → Action
```

## Layout Grid
```
Desktop (1200px+):  12 columns, 24px gutter, 80px margin
Tablet (768-1199):  8 columns, 16px gutter, 40px margin
Mobile (< 768):     4 columns, 16px gutter, 16px margin
```

Max content width: 1280px (center on larger screens)

## Dark Mode Rules
- Don't just invert colors — dark backgrounds need reduced contrast (#F0F0F0 not #FFFFFF)
- Elevation = lighter surface (not shadows, which are invisible on dark)
- Reduce image brightness slightly (filter: brightness(0.9))
- Test all semantic colors on both backgrounds
- Use CSS variables / design tokens for easy switching

## Visual Hierarchy Checklist
For any screen, verify:
1. **What should the user see first?** (size, color, position)
2. **What's the primary action?** (most prominent button/CTA)
3. **Is the reading order logical?** (top→bottom, left→right)
4. **Can I remove anything?** (every element earns its place)
5. **Is whitespace sufficient?** (breathing room = clarity)

## Responsive Design Rules
- **Mobile-first**: Design small, then add complexity for larger screens
- **Thumb zone**: Primary actions in bottom half on mobile
- **Touch targets**: Minimum 44x44px
- **Text**: Never smaller than 14px on mobile
- **Images**: Use srcset for multiple resolutions
- **Tables**: Horizontal scroll or card layout on mobile (never squish)

## Handoff to Development
Provide:
```markdown
## Component: [Name]
- Design tokens used: [colors, spacing, typography]
- States: [default, hover, active, focus, disabled, error]
- Responsive behavior: [how it changes at breakpoints]
- Animations: [duration, easing, what moves]
- Accessibility: [focus order, ARIA, contrast ratios]
```

## Handoff Points
- **← From UX Designer**: Receives wireframes, interaction specs, user flows
- **← From Copywriter**: Receives final copy for UI text
- **→ Frontend Dev**: Provides visual specs, design tokens, component designs
- **→ UX Designer**: Feedback on feasibility of interaction patterns
- **← From Brand/PM**: Receives brand guidelines and constraints
