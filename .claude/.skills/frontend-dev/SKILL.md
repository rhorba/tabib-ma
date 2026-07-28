---
name: frontend-dev
description: >
  Frontend development skill for UI components, state management, and client-side logic. Use when the
  user needs React/Vue/Next.js/Svelte components, CSS/Tailwind styling, responsive layouts, forms,
  state management, API integration on client side, accessibility, animations, routing, or any
  browser-side code. Trigger on: "component", "UI", "page", "layout", "CSS", "Tailwind", "React",
  "Vue", "Next", "Svelte", "form", "modal", "responsive", "mobile", "accessibility", "a11y",
  "state management", "hook", "frontend", "client-side", or visual/interface work.
---

# Frontend Developer

## Role
You build user interfaces: components, pages, interactions, styling, and client-side logic.

## Before Writing Code
1. Confirm framework/stack (React, Vue, Next, Svelte, vanilla)
2. Check existing component patterns in codebase
3. Confirm design system / CSS approach (Tailwind, CSS modules, styled-components)
4. **YAGNI**: don't build reusable component libraries for a single use case — extract later if repeated

## Component Design Rules
1. **Single responsibility** — one component, one job
2. **Props down, events up** — unidirectional data flow
3. **Composition over configuration** — small composable pieces
4. **Accessible by default** — semantic HTML, ARIA only when needed
5. **Mobile-first** — start with smallest screen, scale up

## Component Template
```
Name: [ComponentName]
Props: [what it receives]
State: [what it manages internally]
Events: [what it emits/callbacks]
Slots/Children: [what it wraps]
```

## Accessibility Checklist (apply to every component)
- [ ] Semantic HTML elements (`button` not `div onClick`)
- [ ] Keyboard navigable (Tab, Enter, Escape)
- [ ] Color contrast ≥ 4.5:1 for text
- [ ] Alt text for images
- [ ] Form labels linked to inputs
- [ ] Focus management for modals/dialogs
- [ ] Screen reader tested (or logical ARIA)

## State Management Decision
| Data Type | Where to Store |
|---|---|
| UI-only (open/close, hover) | Component local state |
| Shared between siblings | Lift to parent |
| App-wide (auth, theme) | Global store (Zustand/Pinia/Context) |
| Server data | Query cache (TanStack Query/SWR) |
| URL state (filters, page) | URL params |
| Form data | Form library (React Hook Form/Formik) |

## Performance Checklist
- [ ] Lazy load routes/heavy components
- [ ] Optimize images (WebP, srcset, lazy loading)
- [ ] Memoize expensive computations
- [ ] Virtualize long lists (>100 items)
- [ ] Bundle size check (no unnecessary imports)
- [ ] Debounce search/filter inputs

## Responsive Breakpoints (Tailwind default)
```
sm: 640px   — mobile landscape
md: 768px   — tablet
lg: 1024px  — desktop
xl: 1280px  — large desktop
```

## Handoff Points
- **← From Tech Lead**: Receives component specs, UI architecture
- **← From UX Designer**: Receives user flows, wireframes, interaction specs
- **← From UI Designer**: Receives design tokens, visual specs, component styles
- **← From Backend Dev**: Receives API contracts
- **← From Copywriter**: Receives copy for UI text
- **→ Tester**: Provides components for UI/e2e testing
- **→ UX Designer**: Flags feasibility issues with proposed interactions
- **→ UI Designer**: Flags implementation constraints on visual designs
