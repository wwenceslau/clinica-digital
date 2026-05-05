# Clínica Digital — Frontend Shell

React 19 + TypeScript 5 + Vite frontend implementing the multi-tenant clinical Shell.

---

## Overview

The Shell is the root layout responsible for navigation, theming, telemetry, and per-tenant session state. It is composed of three main building blocks:

| Layer | Component | Description |
|-------|-----------|-------------|
| Template | `MainTemplate` | Shell root — owns provider stack, telemetry, and lazy Sidebar |
| Organism | `Header` | Top app-bar with location selector and practitioner profile |
| Molecule | `LocationSelector` | WAI-ARIA combobox for active location switching |
| Molecule | `PractitionerProfile` | Current practitioner name + role chip |
| Molecule | `ShellFooter` | Debug overlay showing trace/tenant metadata |

---

## Quick Start

```bash
npm install
npm run dev       # Vite dev server on http://localhost:5173
npm test          # Vitest unit tests
npm run lint      # ESLint
npm run e2e       # Playwright E2E
```

---

## Shell Integration Guide

### 1. Mounting `MainTemplate`

`MainTemplate` is the only entry point for the Shell. Pass a `MainTemplateProps` object that carries session + tenant context:

```tsx
import { MainTemplate } from "./components/templates/MainTemplate";

<MainTemplate
  trainingContext={{
    user_id: session.practitioner.id,
    trace_id: crypto.randomUUID(),
    tenant_id: tenant.tenantId,
    debug_mode: import.meta.env.DEV,
  }}
  navigationSchema={schema}
  onNavigate={(route) => navigate(route)}
>
  <Outlet />
</MainTemplate>
```

`MainTemplate` internally wraps all children in:
- `ThemeContextProvider` — theme token + dark-mode toggle (keyed by `user_id`)
- `LocaleContextProvider` — locale preference persistence (keyed by `user_id`)
- `ShellContextProvider` — shell-wide context value broadcast

### 2. `Header` organism

`Header` renders the top app-bar with tenant name, active location selector, and practitioner profile. It is mounted automatically by `MainTemplate`, but can be used standalone in tests:

```tsx
import { Header } from "./components/organisms/Header";

<Header
  context={{
    tenant_id: "tenant-abc",
    tenant_name: "Clínica Exemplo",
    practitioner_id: "pract-1",
    practitioner_name: "Dr. João",
    practitioner_role: "physician",
    available_locations: [{ id: "loc-1", name: "Unidade Centro" }],
    active_location_id: "loc-1",
  }}
  onLocationChange={(locationId) => console.log("selected:", locationId)}
/>
```

**Tenant safeguard**: `Header` returns `null` if `tenant_id` is falsy, preventing cross-tenant data leakage.

### 3. `LocationSelector` molecule

`LocationSelector` is a WAI-ARIA combobox rendered inside `Header`. It persists the selected location to `localStorage` via `locationPersistence.ts`:

```tsx
import { LocationSelector } from "./components/molecules/LocationSelector";

<LocationSelector
  locations={[{ id: "loc-1", name: "Unidade Centro" }]}
  activeLocationId="loc-1"
  onLocationChange={(id) => persistLocationId(userId, tenantId, id)}
/>
```

`data-testid="header-location-selector"` is set on the root element for test queries.

### 4. `ShellFooter` molecule

`ShellFooter` is a fixed debug overlay rendered at the bottom of the viewport. It is **only visible when `debugMode=true`** and carries telemetry DOM attributes for E2E inspection:

```tsx
import { ShellFooter } from "./components/molecules/ShellFooter";

<ShellFooter
  traceId={trainingContext.trace_id}
  tenantId={effectiveTenantId}
  debugMode={trainingContext.debug_mode}
/>
```

When `debugMode` is `false`, `ShellFooter` returns `null` and has no DOM footprint.

---

## CSS Token Architecture

Global design tokens are defined in `src/index.css` using CSS custom properties. All component styling **must use these tokens**; per-component inline styles are prohibited.

| Token | Alias | Description |
|-------|-------|-------------|
| `--shell-color-primary` | `--primary` | Brand primary colour |
| `--canvas` | `--background` | Page background |
| `--ink` | — | Default text colour |
| `--shell-color-focus` | — | Focus ring colour (a11y) |
| `--shell-spacing-unit` | — | Base spacing unit (8px) |

Dark mode is activated by setting `data-theme="dark"` on `:root`. Toggle via `ThemeContextProvider`.

---

## Telemetry

The Shell emits structured telemetry events via `emitShellTelemetry()` in `src/services/observability.ts`:

| Event | Trigger |
|-------|---------|
| `shell.render` | `MainTemplate` mounts |
| `shell.location.changed` | User selects a different location |

The Shell root `<Box>` carries `data-trace-id` and `data-tenant-id` DOM attributes for trace correlation.

---

## i18n

Shell UI strings are organised by namespace in `src/i18n/shell-namespaces.ts`. Header labels, sidebar items, and telemetry descriptions all use keys from `headerNamespaceKeys`. Add translations under `src/i18n/locales/`.

---

## Testing

| Command | Scope |
|---------|-------|
| `npm test` | All Vitest unit tests |
| `npx vitest run src/test/ShellFooter.test.tsx` | ShellFooter unit tests |
| `npx vitest run src/test/MainTemplateProviders.test.tsx` | Provider stack unit tests |
| `npx vitest run src/test/ThemeTokens.test.tsx` | CSS token tests |
| `npm run e2e` | Playwright E2E suite |

Test utilities live in `src/test/`:
- `renderWithShellProviders.tsx` — wraps UI in the full provider chain for unit tests
- `setup.ts` — global setup with `afterEach(cleanup)` for jsdom isolation
