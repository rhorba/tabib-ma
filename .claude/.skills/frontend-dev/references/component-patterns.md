# Frontend Dev — Component Patterns & State Management Reference

Load when designing component architecture, managing state, or reviewing accessibility.

---

## Component Design Principles

1. **Single responsibility** — one component, one job
2. **Composition over inheritance** — compose small components into complex UIs
3. **Props down, events up** — data flows one way; children emit events
4. **Controlled components** — form state lives in the parent or store, not the input
5. **Colocate state** — lift state up only as far as needed, no further

---

## Component Patterns

### Container / Presentational Split
```tsx
// Presentational — pure display, no data fetching
function OrderCard({ order, onCancel }: OrderCardProps) {
  return (
    <div>
      <h3>{order.id}</h3>
      <button onClick={() => onCancel(order.id)}>Cancel</button>
    </div>
  );
}

// Container — data fetching + state
function OrderCardContainer({ orderId }: { orderId: string }) {
  const { data: order } = useOrder(orderId);
  const { mutate: cancel } = useCancelOrder();
  return <OrderCard order={order} onCancel={cancel} />;
}
```

### Compound Component
```tsx
// Usage: <Tabs> acts as context provider; children self-register
<Tabs defaultTab="profile">
  <Tabs.List>
    <Tabs.Tab id="profile">Profile</Tabs.Tab>
    <Tabs.Tab id="security">Security</Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel id="profile"><ProfileForm /></Tabs.Panel>
  <Tabs.Panel id="security"><SecurityForm /></Tabs.Panel>
</Tabs>
```

### Render Props / Custom Hook (prefer hook)
```tsx
// Custom hook encapsulates logic, component stays clean
function useToggle(initial = false) {
  const [on, setOn] = useState(initial);
  const toggle = useCallback(() => setOn(v => !v), []);
  return { on, toggle };
}

function DisclosureButton() {
  const { on: isOpen, toggle } = useToggle();
  return <button onClick={toggle}>{isOpen ? "Close" : "Open"}</button>;
}
```

---

## State Management Decision Tree

```
Is the state used only in one component?
  YES → useState / useReducer (local state)
  NO → Is it server/remote data?
         YES → React Query / SWR / RTK Query
         NO → Is it global UI state (theme, modals, auth)?
                YES → Zustand / Jotai / Context (lightweight)
                YES (complex) → Redux Toolkit (large apps)
```

**Golden rule**: Don't reach for Redux until useState + prop drilling becomes genuinely painful.

---

## Form Patterns

### Controlled Input (React)
```tsx
function EmailField() {
  const [value, setValue] = useState("");
  const [error, setError] = useState("");

  function validate(v: string) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? "" : "Invalid email";
  }

  return (
    <div>
      <input
        type="email"
        value={value}
        aria-invalid={!!error}
        aria-describedby="email-error"
        onChange={e => {
          setValue(e.target.value);
          setError(validate(e.target.value));
        }}
      />
      {error && <span id="email-error" role="alert">{error}</span>}
    </div>
  );
}
```

Use react-hook-form for complex forms — avoids re-renders on each keystroke.

---

## Accessibility Checklist (WCAG 2.1 AA)

### Every interactive element
- [ ] Has a visible focus indicator (no `outline: none` without replacement)
- [ ] Operable by keyboard alone (Tab, Enter, Space, Arrow keys)
- [ ] Has an accessible name (`aria-label`, `aria-labelledby`, or visible text)

### Images
- [ ] Decorative images: `alt=""`
- [ ] Informative images: descriptive `alt` text

### Forms
- [ ] Every input has a `<label>` (associated via `for` / `id`)
- [ ] Error messages linked with `aria-describedby`
- [ ] Required fields marked with `aria-required="true"`

### Color & contrast
- [ ] Text contrast ≥ 4.5:1 (normal text), ≥ 3:1 (large text)
- [ ] No information conveyed by color alone

### Dynamic content
- [ ] Loading states announced: `aria-live="polite"`
- [ ] Modals trap focus; restore focus on close
- [ ] Route changes announced to screen readers

---

## Performance Patterns

### Code Splitting
```tsx
// Lazy-load heavy routes
const Dashboard = lazy(() => import("./pages/Dashboard"));
<Suspense fallback={<Spinner />}><Dashboard /></Suspense>
```

### Memoization (use sparingly — measure first)
```tsx
// Prevent re-render of expensive child when parent re-renders
const MemoizedList = memo(ExpensiveList);

// Stable function reference across renders
const handleClick = useCallback(() => onSelect(id), [id, onSelect]);

// Cache expensive computation
const sorted = useMemo(() => items.sort(compareFn), [items]);
```

**Rule**: Profile before memoizing. Premature memoization adds complexity with no benefit.

---

## CSS / Styling Conventions

```
Utility-first (Tailwind):    Fast, consistent, no naming decisions needed
CSS Modules:                 Scoped, works with any CSS feature
Styled-components / Emotion: Co-located styles, dynamic props, good DX
Plain CSS / SASS:            Familiar, no build complexity for small projects
```

**Component styling rules:**
- No global style overrides — scope everything to the component
- Design tokens for colors, spacing, typography — never hardcode hex values
- Mobile-first breakpoints: `sm: 640px  md: 768px  lg: 1024px  xl: 1280px`
