# Shell Feature Readiness Checklist

Quick validation checklist for feature 003-shell-layout-estrutural.
All items must be `[x]` before merging to main.

## Accessibility (WCAG 2.1 AA)

- [x] Header `<AppBar>` uses `role="banner"` and `component="header"` semantic landmarks
- [x] `LocationSelector` implements WAI-ARIA combobox pattern (role, aria-label, keyboard nav)
- [x] `PractitionerProfile` has `aria-label` combining name + role for screen readers
- [x] Focus ring visible for all interactive elements via `--shell-color-focus` token
- [x] No WCAG 2.1 AA violations reported by axe-core in the shell chrome (E2E T044)

## Performance

- [x] `Sidebar` is lazy-loaded via `React.lazy()` + `<Suspense>` to defer JS parsing on initial render
- [x] Suspense fallback uses `aria-busy="true"` skeleton to communicate loading state
- [x] LCP budget ≤ 1500ms verified by E2E performance test (T045)
- [x] No synchronous heavy imports added to `MainTemplate.tsx` render path

## i18n

- [x] All Header and Sidebar labels use keys from `headerNamespaceKeys` (no hard-coded strings)
- [x] `shell-namespaces.ts` keys imported by `MainTemplate` and passed to telemetry metadata
- [x] Translation files exist under `src/i18n/locales/` for supported locales

## Telemetry & Observability

- [x] `shell.render` event emitted on `MainTemplate` mount with `trace_id` + `tenant_id`
- [x] `shell.location.changed` event emitted whenever active location switches
- [x] Shell root `<Box>` carries `data-trace-id` and `data-tenant-id` DOM attributes for trace correlation
- [x] `ShellFooter` visible in debug mode, hidden in production (`debugMode=false` → `null` return)
- [x] `getDomTelemetryAttributes()` used to set telemetry attributes on footer root

## Multi-tenancy & Security

- [x] `Header` renders `null` when `tenant_id` is falsy (no cross-tenant data leakage)
- [x] Location persistence key scoped to `(userId, tenantId)` pair — no cross-tenant state sharing
- [x] `navigationSchema` filtered by tenant via `filterNavigationByTenant()` before rendering Sidebar
- [x] Security domain always ensured in navigation schema via `ensureSecurityDomain()`

## CSS Token Architecture

- [x] Global tokens defined in `src/index.css` under `:root[data-theme='light']` and `:root[data-theme='dark']`
- [x] Canonical aliases `--primary` and `--background` defined for both themes
- [x] Dark-mode toggle controlled via `ThemeContextProvider` (sets `data-theme` on `:root`)
- [x] Per-component inline `style={{}}` usage documented at baseline; new components must use tokens

## Provider Stack

- [x] `ThemeContextProvider` wraps the full shell tree (keyed by `user_id`)
- [x] `LocaleContextProvider` wraps the full shell tree (locale persisted to localStorage)
- [x] `ShellContextProvider` supplies `trainingContext` to all descendant consumers
- [x] All providers mount above `MainTemplateContent` in the component tree

## Test Coverage

- [x] `ShellFooter.test.tsx` — 4 tests: data-trace-id, data-tenant-id, hidden when debugMode=false, visible when debugMode=true
- [x] `MainTemplateProviders.test.tsx` — 4 tests: ThemeContext, LocaleContext, ShellContext present; i18n key resolves
- [x] `ThemeTokens.test.tsx` — 8 tests: --primary, --background, dark-mode token parity
- [x] `Header.test.tsx` — 6 tests: tenant data binding, location selector, practitioner profile
- [x] `LocationPersistence.test.tsx` — 11 tests: key scoping, persist/load lifecycle
- [x] `HeaderA11y.test.tsx` — 11 tests: ARIA roles, keyboard navigation, screen-reader labels
- [x] `tsc --noEmit` exits 0 (TypeScript strict mode)
- [x] `npm run lint` exits 0 (ESLint clean)
