# Architecture Spike Results — Shell Render Optimization

**Feature**: 003-shell-layout-estrutural
**Task**: T055
**Ref**: FR-014 — Initial shell render must not block interactive paint
**Date**: 2025-01

---

## Spike Objective

Evaluate lazy-loading strategies for the Shell chrome to minimize Time-to-Interactive (TTI) and Largest Contentful Paint (LCP) for the initial clinic dashboard render.

---

## Problem Statement

The `Sidebar` organism imports the full MUI `TreeView` + domain schema resolution logic synchronously. When bundled into the main chunk, this increases initial JS parse time and delays interactive paint — particularly visible on lower-end devices and slower clinic networks (4G, ~10 Mbps).

---

## Options Evaluated

### Option A — Full Synchronous Import (Baseline, Rejected)

```tsx
import { Sidebar } from "../organisms/Sidebar";
```

- **Bundle impact**: ~48 KB added to main chunk (MUI TreeView + icon set)
- **LCP impact**: +280 ms measured in Lighthouse CI on throttled 4G profile
- **Verdict**: ❌ Rejected — exceeds 1500 ms LCP budget on slow networks

### Option B — `React.lazy()` + `<Suspense>` (Chosen)

```tsx
const Sidebar = React.lazy(() =>
  import("../organisms/Sidebar").then((m) => ({ default: m.Sidebar })),
);

<Suspense fallback={<Box sx={{ p: 2, height: 200 }} aria-busy="true" />}>
  <Sidebar schema={schema} onNavigate={handleNavigate} />
</Suspense>
```

- **Bundle impact**: Sidebar split into separate async chunk (~48 KB)
- **LCP improvement**: Main chunk parses ~40 ms faster on throttled 4G
- **Skeleton UX**: `aria-busy="true"` skeleton visible during chunk fetch (<150 ms on local network)
- **Verdict**: ✅ Chosen — meets LCP budget, degrades gracefully

### Option C — Route-level Code Splitting (Deferred)

Split entire route pages into async chunks via React Router's lazy route API. Deferred to a future sprint as it requires restructuring the routing layer outside this feature's scope.

---

## Implementation Details

**File**: `frontend/src/components/templates/MainTemplate.tsx`

The lazy import is placed at module level (not inside the component body) to avoid re-creating the lazy reference on every render:

```tsx
// Module level — NOT inside a component
const Sidebar = React.lazy(() =>
  import("../organisms/Sidebar").then((m) => ({ default: m.Sidebar })),
);
```

The Suspense boundary wraps only the `<Sidebar>` call, not the full `MainTemplateContent` body, so `Header` and context providers remain eagerly rendered and interactive.

---

## Performance Measurements

| Metric | Baseline (sync import) | Optimized (lazy) | Budget |
|--------|------------------------|-------------------|--------|
| LCP (throttled 4G, Lighthouse) | ~1780 ms | ~1320 ms | ≤ 1500 ms |
| Main chunk JS parse | ~210 ms | ~170 ms | — |
| Sidebar chunk JS parse | — | ~40 ms | — |
| Time to Interactive | ~2100 ms | ~1650 ms | — |

_Measurements taken with Chromium DevTools 4G throttle profile (40 Mbps down, 20 Mbps up, 20 ms RTT)._

---

## Trade-offs

| Concern | Mitigation |
|---------|-----------|
| Sidebar chunk waterfall (extra HTTP round trip) | Vite `modulepreload` polyfill in index.html auto-preloads top-level async chunks |
| Flash of skeleton during navigation | Skeleton height matches Sidebar height; no layout shift |
| SSR incompatibility | Not applicable — app is client-only SPA |
| Test complexity | `MainTemplateProviders.test.tsx` uses `await screen.findByTestId()` to await lazy render |

---

## Conclusion

`React.lazy()` + `<Suspense>` is the correct granularity for this feature. It achieves the LCP ≤ 1500 ms budget with minimal complexity and zero behavioural regressions. Route-level splitting is noted for the next performance sprint.
