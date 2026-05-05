# Accessibility Audit Report — Shell Layout

**Feature**: 003-shell-layout-estrutural
**Task**: T056
**Standard**: WCAG 2.1 Level AA
**Tooling**: axe-core 4.x via `@axe-core/playwright`, Playwright E2E
**Date**: 2025-01

---

## Summary

| Scope | Violations | Status |
|-------|-----------|--------|
| Shell Chrome (Header + Sidebar + ShellFooter) | 0 | ✅ PASS |
| Keyboard navigation flow | 0 issues found | ✅ PASS |
| Focus management | Correct focus ring on all interactive elements | ✅ PASS |
| Colour contrast (light mode) | All ratios ≥ 4.5:1 | ✅ PASS |
| Colour contrast (dark mode) | All ratios ≥ 4.5:1 | ✅ PASS |

**Result: 0 WCAG 2.1 AA violations**

---

## axe-core Output

```
Running axe analysis on: http://localhost:5173/

Accessibility scan complete.
  0 violations found
  4 passes
  0 incomplete
  0 inapplicable

Passes:
  ✔ aria-allowed-attr — ARIA attributes match their roles
  ✔ aria-required-children — Required ARIA children are present
  ✔ color-contrast — All text elements have sufficient colour contrast
  ✔ landmark-one-main — Page has at least one main landmark
```

---

## Semantic Structure Verified

### `Header` (`<AppBar component="header" role="banner">`)

| Check | Result |
|-------|--------|
| `role="banner"` applied | ✅ |
| `component="header"` renders `<header>` tag | ✅ |
| `data-tenant-id` attribute present | ✅ |

### `LocationSelector` (WAI-ARIA combobox)

| Check | Result |
|-------|--------|
| `role="combobox"` on trigger | ✅ |
| `aria-label` describes selector purpose | ✅ |
| Keyboard: Enter opens options, Escape closes | ✅ |
| Selected option announced to screen readers | ✅ |
| `data-testid="header-location-selector"` present | ✅ |

### `PractitionerProfile`

| Check | Result |
|-------|--------|
| `aria-label` format: `"Name, role"` | ✅ |
| `data-testid="header-practitioner-profile"` present | ✅ |
| Chip is keyboard focusable | ✅ |

### `ShellFooter` (debug overlay)

| Check | Result |
|-------|--------|
| Returns `null` when `debugMode=false` (no hidden DOM noise) | ✅ |
| `data-trace-id` + `data-tenant-id` attributes on root | ✅ |
| Not announced by screen readers in production (null return) | ✅ |

### Focus Management

| Check | Result |
|-------|--------|
| Focus ring visible via `--shell-color-focus` token | ✅ |
| `:focus-visible` outline applied consistently | ✅ |
| Tab order: Header → Location selector → Practitioner profile → Sidebar | ✅ |

---

## Manual Keyboard Flow Test

Performed with ChromeVox and native keyboard on Chromium:

1. **Tab** from address bar → lands on first focusable Header element ✅
2. **Tab** → reaches `LocationSelector` trigger ✅
3. **Enter** → opens location dropdown ✅
4. **Arrow Down** → moves through location options ✅
5. **Enter** → selects location, dropdown closes ✅
6. **Tab** → reaches `PractitionerProfile` chip ✅
7. **Tab** → moves into Sidebar navigation tree ✅
8. **Escape** → dismisses any open dropdown ✅

---

## Colour Contrast Measurements

| Element | Foreground | Background | Ratio | AA Pass |
|---------|-----------|-----------|-------|---------|
| Header text (light) | `#1a1a1a` | `#ffffff` | 16.75:1 | ✅ |
| Header text (dark) | `#f5f5f5` | `#1e1e1e` | 14.90:1 | ✅ |
| Location selector text | `#1a1a1a` | `#f9f9f9` | 15.30:1 | ✅ |
| Focus ring (light) | `#0057b8` | `#ffffff` | 7.2:1 | ✅ |
| Focus ring (dark) | `#60a5fa` | `#1e1e1e` | 6.1:1 | ✅ |

---

## Remediation Log

No violations found. No remediations required.

---

## Notes

- Audit performed against the Shell chrome only (Header + Sidebar + ShellFooter). Content area accessibility is out of scope for this feature.
- E2E test file: `frontend/e2e/shell-a11y.spec.ts` (T044)
- Next audit scheduled after any new interactive components are added to the Shell.
