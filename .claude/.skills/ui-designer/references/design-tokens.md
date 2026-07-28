# UI Designer — Design Tokens & Visual Design Reference

Load when defining a design system, setting up tokens, or making visual design decisions.

---

## Design Token Taxonomy

Design tokens are the single source of truth for all visual decisions.
They cascade: Primitive → Semantic → Component.

```
Primitive tokens (raw values):
  color.blue.500 = #3B82F6
  space.4 = 16px

Semantic tokens (purpose-named, reference primitives):
  color.action.primary = {color.blue.500}
  space.component.padding = {space.4}

Component tokens (component-specific):
  button.primary.background = {color.action.primary}
  button.primary.padding-x = {space.component.padding}
```

Never hardcode hex values or pixel values directly in component styles.
Always reference a token.

---

## Color System

### Palette Structure
```
[Brand]       — primary, secondary, accent
[Neutral]     — gray scale from 50 (lightest) → 950 (darkest)
[Semantic]    — success, warning, error, info
[Background]  — base, surface, overlay
[Text]        — primary, secondary, disabled, on-dark
[Border]      — default, focus, error
```

### Minimum Contrast Ratios (WCAG AA)
```
Normal text (< 18px regular / < 14px bold): 4.5:1
Large text (≥ 18px regular / ≥ 14px bold): 3:1
Interactive elements (buttons, inputs): 3:1 against background
Focus indicators: 3:1 against adjacent color
```

Tools: coolors.co/contrast-checker, webaim.org/resources/contrastchecker

---

## Typography Scale

Use a modular scale (ratio 1.25 or 1.333):

```
text-xs:   12px / 0.75rem
text-sm:   14px / 0.875rem
text-base: 16px / 1rem       ← base (never go below this for body)
text-lg:   18px / 1.125rem
text-xl:   20px / 1.25rem
text-2xl:  24px / 1.5rem
text-3xl:  30px / 1.875rem
text-4xl:  36px / 2.25rem
text-5xl:  48px / 3rem
```

**Line heights:**
```
body copy:    1.5–1.7
headings:     1.1–1.3
UI elements:  1.0–1.25
```

**Font weight convention:**
```
400 — Regular (body)
500 — Medium (labels, UI)
600 — Semibold (headings, emphasis)
700 — Bold (display, CTA)
```

---

## Spacing Scale (4px base grid)

```
space-1:  4px
space-2:  8px
space-3:  12px
space-4:  16px
space-5:  20px
space-6:  24px
space-8:  32px
space-10: 40px
space-12: 48px
space-16: 64px
space-20: 80px
space-24: 96px
```

Always use multiples of 4. Never use arbitrary pixel values like `13px` or `27px`.

---

## Shadow / Elevation Scale

```
shadow-sm:  0 1px 2px rgba(0,0,0,0.05)          ← cards, inputs
shadow-md:  0 4px 6px rgba(0,0,0,0.07)          ← dropdowns, popovers
shadow-lg:  0 10px 15px rgba(0,0,0,0.10)        ← modals, floating panels
shadow-xl:  0 20px 25px rgba(0,0,0,0.15)        ← tooltips, highest elevation
```

Use elevation consistently — higher = more important, more temporary.

---

## Breakpoints (mobile-first)

```
sm:  640px  — large phone (landscape)
md:  768px  — tablet
lg:  1024px — small laptop
xl:  1280px — desktop
2xl: 1536px — large desktop
```

Design for mobile first. Expand layout at each breakpoint, don't shrink.

---

## Component Visual Checklist

### Buttons
- [ ] Primary — filled, brand color, high contrast text
- [ ] Secondary — outlined or ghost, same size as primary
- [ ] Destructive — red / error color variant
- [ ] Disabled — 40% opacity or muted, `cursor: not-allowed`
- [ ] Loading — spinner replaces icon or label, width preserved
- [ ] Min touch target: 44×44px

### Form Inputs
- [ ] Default, focus, error, disabled states
- [ ] Focus: 2px outline in brand color (or `outline-offset: 2px`)
- [ ] Error: red border + error message below
- [ ] Label always visible (not placeholder-only)

### Icons
- [ ] Consistent size set: 16px, 20px, 24px
- [ ] Always paired with a text label or `aria-label`
- [ ] Stroke-based or fill-based — never mix within a product

---

## Dark Mode Token Mapping

```css
:root {
  --color-background: #FFFFFF;
  --color-text-primary: #111827;
  --color-border: #E5E7EB;
}

[data-theme="dark"] {
  --color-background: #111827;
  --color-text-primary: #F9FAFB;
  --color-border: #374151;
}
```

All component styles reference variables — swapping the theme swaps every value.
