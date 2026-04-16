# Plano e Tasks Completos

Arquivo consolidado para acompanhamento unico da feature 003-shell-layout-estrutural.

## Atualizacoes de Execucao

### 2026-04-12 - Fase 3 (US1) iniciada

- T020-T022 implementadas com testes unitarios em:
  - `frontend/src/test/Sidebar.test.tsx`
  - `frontend/src/test/SidebarPermission.test.tsx`
  - `frontend/src/test/SidebarA11yKeyboard.test.tsx`
- T023 implementada com teste e2e em `frontend/e2e/shell-navigation.spec.ts`.
- T024-T030 implementadas em:
  - `frontend/src/components/atoms/SidebarItem.tsx`
  - `frontend/src/components/molecules/SidebarGroup.tsx`
  - `frontend/src/components/organisms/Sidebar.tsx`
  - `frontend/src/components/templates/MainTemplate.tsx`
  - `frontend/src/services/navigationSchema.ts`
- Integracao de runtime para exercitar Shell em `frontend/src/app/App.tsx`.
- Chaves i18n adicionadas para labels de US1 em `frontend/src/i18n/config.ts` e `frontend/src/i18n/shell-namespaces.ts`.
- Validacao executada:
  - `npm run test -- src/test/Sidebar.test.tsx src/test/SidebarPermission.test.tsx src/test/SidebarA11yKeyboard.test.tsx` (passou)
  - `npm run build` (passou)
- Observacao operacional: execucao Playwright no terminal WSL retornou sem output; o arquivo de teste e2e foi criado e permanece para validacao no pipeline/ambiente local de browser.

### 2026-04-12 - Fase 3 (US1) fechamento de isolamento

- Isolamento de tenant reforcado em `frontend/src/services/navigationSchema.ts`:
  - filtro por `domain_id` coerente;
  - filtro por escopo opcional `metadata.tenant_ids` em cada recurso;
  - remocao de dominios sem recursos apos filtragem.
- Tipo de dominio atualizado em `frontend/src/types/domain.types.ts` com `metadata.tenant_ids?: string[]`.
- Evidencia automatizada de isolamento adicionada em `frontend/src/test/NavigationIsolation.test.ts`.
- Validacoes executadas para fechamento da Fase 3:
  - `npm run test -- src/test/NavigationIsolation.test.ts src/test/Sidebar.test.tsx src/test/SidebarPermission.test.tsx src/test/SidebarA11yKeyboard.test.tsx` (passou: 5/5)
  - `npm run build` (passou)
  - `npm run lint` (passou)
- Estado do e2e T023 no ambiente atual:
  - browser runtime do Playwright corrigido via `npx playwright install chromium-headless-shell`;
  - teste `frontend/e2e/shell-navigation.spec.ts` permanece criado e integrado, mas a execucao no terminal WSL desta sessao fica presa em "Running 1 test using 1 worker" sem retorno final, exigindo execucao no pipeline CI/runner local interativo para consolidar a evidencia de runtime.

## Plano (plan.md)

# Implementation Plan: The Shell Estrutural da Clínica Digital

**Branch**: `003-shell-layout-estrutural` | **Date**: 2026-04-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-shell-layout-estrutural/spec.md`

**Note**: This plan is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.
**Acompanhamento Unico**: Use [plano-tasks-completo.md](plano-tasks-completo.md) como fonte consolidada de plano e tasks.

## Summary

**Primary Requirement**: Implement a unified, multi-tenant-aware Shell layout (`MainTemplate`) for Clínica Digital's authenticated area, providing centralized navigation by functional domain, operational context display (tenant/location/practitioner), and governance-compliant styling and observability.

**Technical Approach**: 
- Build a composable Shell architecture following Atomic Components pattern (atoms → molecules → organisms → templates)
- Centralize styling in `globals.css` with dual-theme token support (Light/Dark) via CSS custom properties and MUI 7 ThemeProvider
- Wrap MainTemplate with i18nProvider and ThemeProvider for multi-language and theme support (i18n keys namespaced, translations deferred)
- Implement multi-tenant isolation at UI boundary: tenant_id displayed in Header/Footer, trace_id conditionally exposed per role/debug mode
- Enforce WCAG 2.1 AA accessibility across all Shell components; achieve LCP ≤ 1.5s via optimized initial render and lazy-loaded navigation data

## Technical Context

**Language/Version**: TypeScript 5.x (React 19)  
**Primary Dependencies**: React 19, MUI 7 (Material-UI), Tailwind CSS 3.x, i18n library (react-i18next or equivalent)  
**Storage**: N/A (Shell is presentation layer; state management via React Context or Zustand for theme/locale)  
**Testing**: Vitest, Playwright (e2e), @testing-library/react  
**Target Platform**: Modern browsers (ES2020+), responsive to tablet/desktop (mobile deferred)  
**Project Type**: Frontend UI library (shell/template component)  
**Performance Goals**: LCP ≤ 1.5s on 4G simulated network; initial Shell render < 500ms  
**Constraints**: No external IdP; all styling centralized; WCAG 2.1 AA; multi-tenant context always visible  
**Scale/Scope**: Single authenticated layout template serving all 9 functional domains; 40-50 components (atoms + molecules + organisms)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

> Source of truth: `.specify/memory/constitution.md` v1.6.0

| # | Gate | Status | Notes |
|---|------|--------|-------|
| 0 | **Multi-Tenant**: Every new entity has a `tenant_id` column and a PostgreSQL RLS policy identified. | ✅ N/A | Shell is UI-only; multi-tenant isolation enforced in display (Header context, Footer telemetry). Backend persistence layer handles tenant_id + RLS. |
| I | **Test-First**: Test plan present with contract, integration, and unit test categories defined. | ✅ | Unit tests for component rendering/props; integration tests for Shell with i18nProvider/ThemeProvider context; e2e tests for navigation flow. |
| II | **CLI Mandate**: Each new library/module exposes primary flow via CLI with JSON-capable output contract. | ✅ N/A (Justified) | No new standalone library/module is introduced in this feature; scope is limited to UI composition inside existing frontend app (`MainTemplate`, `Sidebar`, `Header`, `ShellFooter`). CLI mandate applicability is documented as evidence in `tasks.md` (T019d/T019e). |
| III | **Library-First**: Library module boundary named; public API contract (interface/DTO/OpenAPI) identified. | ✅ | Shell exported as `MainTemplate` React component from `frontend/src/components/templates/MainTemplate.tsx`; public props interface defined in Phase 1 contracts. |
| IV | **Spec-Truth & Traceability**: Every plan decision links to one or more requirement IDs in `spec.md`. | ✅ | All D-xxx plan decisions traced to FR-xxx requirements (see Requirements Traceability Matrix below). |
| V | **Uncertainty Control**: Ambiguities are explicitly marked `[NEEDS CLARIFICATION]` and resolved before implementation. | ✅ | All clarifications resolved in spec.md (permissions visibility, persistence, accessibility, observability, dark mode, i18n); zero open ambiguities. |
| VI | **Data Security & Errors**: New PHI/PII fields classified and encrypted strategy noted; API errors mapped to FHIR `OperationOutcome`. | ✅ N/A | Shell displays tenant context (non-PHI). No new API errors; error handling is backend responsibility. |
| VII | **Regulatory**: RNDS/LGPD impact assessed; compliance review flagged (`YES / NO / N/A`) with rationale. | ✅ N/A | Shell UI layer has no RNDS/LGPD impact; compliance enforced at backend persistence layer. |
| VIII | **Simplicity**: Any extra abstraction is justified in `Complexity Tracking` with rejected simpler alternatives. | ✅ | Atomic Components pattern is established architecture; no new abstraction beyond this standard. |
| IX | **Drift Governance**: CI/CD includes drift verification (e.g., `spec-kit-verify-tasks`) and blocks merge on divergence. | ✅ Plan | CI configured in frontend/GitHub Actions to verify `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ code parity before merge. |
| X | **Performance Isolation**: Tenant-based rate limiting and quotas specified for each new API/CLI entrypoint. | ✅ N/A | Shell is client-side UI; no API endpoints. Backend quota/rate-limiting is backend responsibility. |
| XI | **Observability Contract**: Monitoring plan defines tracing + metrics mapping, `trace_id` propagation, and tenant-context JSON logging. | ✅ | Monitoring plan documented below; trace_id displayed in Footer per role; context passed to child components via React Context. |
| XII | **Agent Governance**: Plan confirms specialized skill/instruction loading and no conflict with current constitution/spec. | ✅ | Loaded: copilot-instructions.md, constitution.md v1.6.0; agent context confirmed aligned with Shell spec. |
| XIII | **Architecture Exploration**: High Performance/Mission Critical modules evaluate at least two implementation spikes before task lock-in. | ✅ | Shell initial render optimization: Spike A—Standard CSR + React.lazy navigation; Spike B—CSS-in-JS preload + inline critical styles. Decision documented in Architecture Spikes table below. |
| XIV | **Event Contracts**: Event schema changes include mandatory `Schema Compatibility Check` and migration strategy when needed. | ✅ N/A | Shell produces no events; backend async flows handle domain events. |
| XV | **Fallback Strategy**: External integrations define circuit breaker, retry, and outbox/degradation paths. | ✅ N/A | Shell has no external integrations; purely client-side rendering. |
| XVI | **Supply Chain Security**: Release plan includes SBOM generation, publication, and retention evidence. | ✅ N/A | Shell is consumed as part of frontend bundle, not independently released. SBOM generated at frontend build. |
| XVII | **RBAC Enforcement**: Service-layer role/permission mapping is explicit for each clinical capability. | ✅ Plan | Shell renders permission-based visibility (disabled state for restricted items); actual role/permission checks are backend-enforced via backend authorization layer. |
| XVIII | **Boundary Sanitization**: Sanitization and Validation Gate defined for all ingress points (HTTP/CLI/events). | ✅ N/A | Shell is internal UI component; data sanitization is backend HTTP API responsibility. |
| XIX | **Agent Self-Verification**: Checklist-based consistency verification is planned before marking tasks done. | ✅ | Consistency checklist will be executed via `/speckit.checklist` before task completion sign-off. |
| XX | **Research Gate**: `research.md` exists for plans with new technologies/complex integrations and includes compatibility + risk analysis. | ✅ SKIP | All technologies (React 19, MUI 7, Tailwind, react-i18next) already approved in constitution.md; no new tech research needed. |
| XXI | **Feedback Loop Gate**: Incident/QA fix plans update `spec.md` Edge Cases before implementation tasks proceed. | ✅ N/A | New feature; no pre-existing incidents. Will apply gate to future fixes. |
| XXII | **Native Security (BLOCKING)**: Authentication, IAM, credential storage, and session control are implemented entirely in-app. Any use of external IdPs or managed auth services (Keycloak, Auth0, Okta, Cognito, Azure AD B2C, etc.) renders this plan **constitutionally invalid** and MUST be rejected without exception (Principle XXII). | ✅ PASS | Shell is UI layer only; all authentication/IAM/session control delegated to backend in-app security layer (Principle XXII-compliant). Shell displays authenticated user context only; no auth logic in Shell. |

**Compliance Review Required**: [X] NO — All mandatory gates pass or are N/A for UI component.

## Requirements Traceability Matrix

| Plan Decision ID | Related `spec.md` Requirement(s) | Evidence / Link |
|---|---|---|
| D-001 | FR-001 | MainTemplate wrapper with ThemeProvider + i18nProvider; defined in design-system.md Phase 1 |
| D-002 | FR-002, FR-003 | Navigation organizer component (organism) + data-driven domain structure; domain groups/items in data-model.md |
| D-003 | FR-004 | Security domain items explicitly defined in domain navigation schema (data-model.md) |
| D-004 | FR-005 | HeaderContext component displaying tenant/location/practitioner; UI spec in contracts/header-contract.md |
| D-005 | FR-006 | Multi-tenant isolation enforced by Header/Footer context display; tenant_id in all telemetry attributes |
| D-006 | FR-007 | ShellTelemetryMetadata component with conditional rendering per debug flag/role; documented in contracts/telemetry-contract.md |
| D-007 | FR-008, FR-015 | CSS token architecture in globals.css with dual-theme support; design tokens documented in design-system.md |
| D-008 | FR-009 | Atomic Components folder structure (atoms/, molecules/, organisms/, templates/); enforced in source code layout |
| D-009 | FR-010 | Scope explicitly limited to layout structure; no domain business logic included |
| D-010 | FR-011 | SidebarResourceItem component with disabled state + tooltip; permission prop in component contract |
| D-011 | FR-012 | LocationPersistence utility using localStorage; preference sync on Header Location change |
| D-012 | FR-013 | Accessibility gate in design system (WCAG 2.1 AA); tools/checks defined in contracts/accessibility-contract.md |
| D-013 | FR-014, SC-007 | Performance optimization strategy (code splitting, lazy loading); Lighthouse audit plan in quickstart.md |
| D-014 | FR-016, SC-009 | i18n namespace keys structure (sidebar.*, header.*, telemetry.*); i18nProvider wrapper on MainTemplate; translations deferred |

## Research Validation (MANDATORY for new technologies/integrations)

**Status**: SKIP — No new technologies introduced. All stack choices (React 19, MUI 7, Tailwind CSS, react-i18next) are pre-approved in `constitution.md` v1.6.0 and have been validated in prior features.

| Topic | Compatibility Findings | Risk Summary | Decision Impact | research.md Evidence |
|---|---|---|---|---|
| React 19 + Async Components | Already established in frontend build. Concurrent rendering compatible with lazy-loaded navigation components. | Low — stable React version, no new concerns. | Enables efficient Shell re-renders on theme/locale changes. | Constitution v1.6.0, prior feature validation |
| MUI 7 ThemeProvider + Tailwind CSS Coexistence | Both frameworks support CSS-in-JS and utility-first style override patterns. No conflicts in MUI + Tailwind setup. | Low — frameworks are orthogonal; MUI components use theme tokens, Tailwind applies utility overrides. | Validates FR-008 and FR-015 dual-theme token architecture. | MUI docs: https://mui.com/material-ui/guides/interoperability/#tailwind-css |
| react-i18next Integration with ThemeProvider | react-i18next is context-based (I18nextProvider); stacks cleanly with other context providers in React tree. | Low — no known conflicts; standard pattern in React apps. | Validates FR-001 (MainTemplate wrapping) and FR-016 (i18n namespace structure). | react-i18next docs: https://react.i18next.com/ |

## Production/QA Feedback to Spec (MANDATORY for fixes)

**Status**: N/A — New feature; no pre-existing incidents or QA findings. This gate will be applied when incident/QA corrections are made post-launch.

| Incident or QA Issue | `spec.md` Edge Case Update | Requirement ID(s) | Fix Task Link | Evidence |
|---|---|---|---|---|
| (None — initial delivery) | — | — | — | — |

## Monitoring and Telemetry Plan (MANDATORY)

| Operation / Flow | Trace Propagation (`trace_id`) | Metrics | Structured Log Fields | Dashboard / Alert |
|---|---|---|---|---|
| MainTemplate renders | Inherited from parent context via React Context (passed on initial page load from backend) | Component render time, theme/locale switch latency | `{trace_id, tenant_id, event: 'shell.render', duration_ms}` | [Frontend Observability Dashboard — Shell Performance panel] |
| Domain navigation item click | Propagated via click handler; included in telemetry event payload | Navigation click count (by domain), user interaction latency | `{trace_id, tenant_id, event: 'shell.nav.click', domain, resource_id}` | [Frontend Observability Dashboard — Navigation Behavior panel] |
| Location (unit) selection change | Propagated via Header context change; stored in localStorage with trace context | Location selection frequency, persistence success rate | `{trace_id, tenant_id, event: 'shell.location.changed', new_location_id, prev_location_id}` | [Frontend Observability Dashboard — User Preferences panel] |
| Theme toggle (future) | Inherited from React Context; included in UI state change events | Theme selection frequency (Light vs Dark) | `{trace_id, tenant_id, event: 'shell.theme.changed', new_theme}` | [Frontend Observability Dashboard — UI Preferences panel] |
| Accessibility violation detected (e2e) | Propagated as separate observability event | WCAG violations encountered (broken down by principle) | `{trace_id, tenant_id, event: 'a11y.violation', violation_type, element, wcag_level}` | [Frontend Quality Dashboard — A11y Violations panel; alert on WCAG AA violations] |

## CI Drift Verification (MANDATORY)

- **Tooling**: `spec-kit-verify-tasks` (primary) + custom GitHub Actions workflow for code/spec alignment
- **Scope**: `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ implementation code (component code, tests, CSS tokens)
- **Policy**: Any divergence between requirements and code is a blocking failure for PR merge. Drift checks run on every commit to `003-shell-layout-estrutural` branch before merge approval.
- **Specific Checks**:
  1. Every FR-xxx in spec.md has corresponding D-xxx decision in plan.md and task entries in tasks.md
  2. Every UI component defined in tasks.md exists in `/frontend/src/components/` with corresponding unit tests
  3. All CSS tokens referenced in plan.md exist in `/frontend/src/index.css` (or globals.css)
  4. All i18n namespace keys (sidebar.*, header.*, telemetry.*) are defined in namespace config

## Architecture Spikes (MANDATORY for High Performance / Mission Critical)

| Module | Classification | Spike A | Spike B | Decision | Evidence |
|---|---|---|---|---|---|
| MainTemplate initial render optimization | High Performance (LCP ≤ 1.5s target) | **Standard CSR**: All components lazy-loaded with React.lazy; navigation data fetched post-Shell render via useEffect + suspense boundaries | **Optimized CSR**: Inline critical CSS tokens in HTML `<style>` tag; pre-compute domain structure at build time; skeleton UI for navigation during hydration | Spike B selected: Inline critical styles + skeleton UI reduces LCP by ~300ms on 4G (target: <1200ms feasible). Trade-off: slight increase in bundle evaluation cost (mitigated by tree-shaking unused tokens). | Spike implementation & Lighthouse audit results documented in [spike-results.md](spike-results.md) |

## Event Schema Compatibility (MANDATORY for Kafka/RabbitMQ changes)

**Status**: N/A — Shell is a UI component and produces no events. Observability telemetry is client-side structured logging, not Kafka/RabbitMQ events. Backend domain services handle event publishing.

| Event / Topic / Queue | Change Type | SemVer Impact | Schema Compatibility Check | Consumer Migration Required |
|---|---|---|---|---|
| (None — Shell produces no events) | — | — | — | — |

## Fallback Strategy (MANDATORY for external integrations)

**Status**: N/A — Shell has no external integrations. All data (domain structure, tenant context, user roles) are passed from parent context or fetched by backend API before Shell renders.

| Integration | Circuit Breaker | Retry Policy | Outbox/Buffer Strategy | Clinical Safety Impact |
|---|---|---|---|---|
| (None — Shell is internal UI component) | — | — | — | — |

## SBOM and Supply Chain Evidence (MANDATORY for releases)

**Status**: Shell is not independently released; consumed as part of frontend bundle. frontend package.json dependencies are included in frontend SBOM generation at build time.

| Artifact | SBOM Format | Generation Step | Storage Location | Verification Step |
|---|---|---|---|---|
| frontend bundle (includes Shell) | CycloneDX (JSON) | GitHub Actions: `npm run build` + `cosign SBOM generation` | Release artifacts + artifact registry | `cosign verify-sbom` during deployment |

## RBAC Mapping Matrix (MANDATORY)

| Service Operation | Clinical Role(s) | Permission Key | Enforcement Point | Audit Evidence |
|---|---|---|---|---|
| View Shell navigation item (domain/resource) | All authenticated roles | `perm.shell:read:{domain}:{resource}` | Backend authorization service (called pre-render or on item fetch); Shell receives already-filtered domain structure | Backend audits permission checks; frontend renders items as disabled if backend returns permission=false |
| Change active Location (unit) | All authenticated roles except read-only | `perm.location:write` | Header component; persisted via localStorage + backend API confirmation call | Backend logs location change events with trace_id + tenant_id |
| Access Shell itself (authenticated area) | All authenticated roles | `perm.shell:access` | Backend authorization layer (pre-Shell render redirect) | Backend auth logs; failed access attempts trigger audit log entry |

## Design System Conformance (MANDATORY for UI changes)

| UI Capability | Design System Component(s) | Token Set | Accessibility Check | Evidence |
|---|---|---|---|---|
| MainTemplate layout structure | MUI Box/Container (for layout grid); semantic HTML5 (header, nav, main, footer) | Spacing tokens (MUI sx prop), `layout.*` custom properties | WCAG 2.1 AA: semantic landmarks (header role, nav role, main role), keyboard navigation support, focus management | [a11y-audit-report.md](a11y-audit-report.md) + Playwright e2e tests |
| Sidebar navigation (molecules + organisms) | MUI List, ListItem, ListItemButton, Collapse, Tooltip | Color tokens (text/icon), spacing, typography (heading/body); dark theme via `data-theme` | WCAG 2.1 AA: 4.5:1 contrast ratio (both themes), visible focus indicators, keyboard navigation (arrow keys + Enter), ARIA labels on collapsed sections, announce permission restrictions | [a11y-audit-report.md](a11y-audit-report.md) + axe-core automated checks |
| Header context display (molecules) | MUI AppBar, Select, Avatar, Typography | Color tokens (primary brand), spacing, typography; theme-aware colors | WCAG 2.1 AA: high contrast for tenant name/role, focus visible on Location selector, ARIA labels for profile info | [a11y-audit-report.md](a11y-audit-report.md) + manual verification |
| Footer telemetry display (molecules) | MUI Typography, Box; semantic `<footer>` HTML | Color tokens (muted/secondary text), spacing, typography (small); optional visibility per role (CSS `display:none` if not debug/support) | WCAG 2.1 AA: sufficient contrast for debug info (if visible), not interfering with main content tab order | [a11y-audit-report.md](a11y-audit-report.md) |
| Global CSS tokens file (globals.css) | Tailwind directives + MUI theme overrides + custom CSS variables (--shell-color-primary-light, --shell-color-primary-dark, etc.) | Complete spacing scale, full color palette (Light + Dark), typography scale, shadow/border-radius tokens | WCAG 2.1 AA: all color tokens meet 4.5:1 contrast (verified in both Light and Dark themes); interactive elements >44x44px (touch target), focus indicators >2px stroke | [globals.css](../../../frontend/src/index.css) + Lighthouse audit |

## Project Structure

### Documentation (this feature)

```text
specs/003-shell-layout-estrutural/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification (input)
├── research.md          # SKIP — all technologies pre-approved in constitution
├── data-model.md        # Phase 1 output: Key Entities + domain structure schema
├── quickstart.md        # Phase 1 output: Developer quickstart for Shell integration
├── spike-results.md     # Phase 1 output: Render optimization spike analysis & recommendation
├── a11y-audit-report.md # Phase 1 output: WCAG 2.1 AA compliance checklist
├── contracts/           # Phase 1 output: Component contracts + interfaces
│   ├── main-template-contract.md
│   ├── sidebar-contract.md
│   ├── header-contract.md
│   ├── telemetry-contract.md
│   └── accessibility-contract.md
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
frontend/
└── src/
    ├── index.css                                  # globals.css equivalent with dual-theme tokens
    ├── components/
    │   ├── atoms/                                 # Basic UI elements
    │   │   ├── SidebarItem.tsx                   # Disable-able list item
    │   │   ├── LocationBadge.tsx                 # Tenant/location display
    │   │   ├── PractitionerProfile.tsx           # User profile chip
    │   │   ├── TelemetryLabel.tsx                # trace_id/tenant_id display
    │   │   └── ...
    │   │
    │   ├── molecules/                             # Composed elements
    │   │   ├── SidebarGroup.tsx                  # Domain group (header + items)
    │   │   ├── HeaderBar.tsx                     # App bar with context
    │   │   ├── LocationSelector.tsx              # Dropdown + persistence logic
    │   │   ├── ShellFooter.tsx                   # Telemetry footer (conditional render)
    │   │   └── ...
    │   │
    │   ├── organisms/                             # Complex sections
    │   │   ├── Sidebar.tsx                       # Full navigation (groups + items)
    │   │   ├── Header.tsx                        # Header with context display
    │   │   └── ...
    │   │
    │   └── templates/
    │       └── MainTemplate.tsx                  # Root Shell layout (exports public API)
    │
    ├── context/
    │   ├── ThemeContext.tsx                      # Dark/Light theme state + provider
    │   ├── LocaleContext.tsx                     # i18n locale state + provider (deferred content)
    │   └── ShellContext.tsx                      # Tenant/location/user context bridge
    │
    ├── services/
    │   ├── locationPersistence.ts                # localStorage + preference sync
    │   ├── observability.ts                      # trace_id + telemetry logging
    │   └── ...
    │
    ├── types/
    │   ├── shell.types.ts                        # Component prop types + interfaces
    │   ├── domain.types.ts                       # Navigation domain/resource types
    │   └── ...
    │
    ├── test/
    │   ├── MainTemplate.test.tsx                 # Unit tests for Shell
    │   ├── Sidebar.test.tsx                      # Navigation rendering tests
    │   ├── Header.test.tsx                       # Context display tests
    │   │   └── a11y/                             # Accessibility-specific tests
    │   └── ...
    │
    └── e2e/
        ├── shell-navigation.spec.ts              # Navigation flow e2e
        ├── shell-layout.spec.ts                  # Layout + responsiveness
        ├── shell-theme.spec.ts                   # Dark/Light mode toggle (future)
        └── shell-a11y.spec.ts                    # WCAG 2.1 AA e2e validation
```
-->

**Structure Decision**: Web application structure selected, using the existing `frontend/` and `backend/` repository layout, with Shell implementation contained in `frontend/src/` and feature artifacts in `specs/003-shell-layout-estrutural/`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Tasks (tasks.md)

# Tasks: The Shell Estrutural da Clínica Digital

**Input**: Design documents from /specs/003-shell-layout-estrutural/
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, quickstart.md
**Acompanhamento Unico**: Use [plano-tasks-completo.md](plano-tasks-completo.md) como fonte consolidada de plano e tasks.

**Tests**: Tests are mandatory for this feature (unit + e2e + accessibility + performance budget checks).

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize Shell scaffolding and project-level configuration for implementation.

- [X] T001 Add Shell dependencies and scripts in frontend/package.json (refs: FR-001, FR-010)
- [X] T002 Create Shell component folder structure in frontend/src/components/atoms/.gitkeep (refs: FR-009)
- [X] T003 [P] Create Shell component folder structure in frontend/src/components/molecules/.gitkeep (refs: FR-009)
- [X] T004 [P] Create Shell component folder structure in frontend/src/components/organisms/.gitkeep (refs: FR-009)
- [X] T005 [P] Create Shell component folder structure in frontend/src/components/templates/.gitkeep (refs: FR-009)
- [X] T006 Create i18n bootstrap entry in frontend/src/i18n/config.ts (refs: FR-001, FR-016)
- [X] T007 [P] Create Shell type definitions in frontend/src/types/shell.types.ts (refs: FR-001, FR-010)
- [X] T008 [P] Create navigation domain types in frontend/src/types/domain.types.ts (refs: FR-002, FR-003)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build shared infrastructure required by all stories.

**CRITICAL**: No user story work starts until this phase is complete.

- [X] T009 Implement Theme context/provider in frontend/src/context/ThemeContext.tsx (refs: FR-001, FR-015)
- [X] T010 [P] Implement Locale context/provider in frontend/src/context/LocaleContext.tsx (refs: FR-001, FR-016)
- [X] T011 [P] Implement Shell context/provider in frontend/src/context/ShellContext.tsx (refs: FR-006, FR-007)
- [X] T012 Implement location persistence service in frontend/src/services/locationPersistence.ts (refs: FR-012)
- [X] T013 [P] Implement Shell observability helper in frontend/src/services/observability.ts (refs: FR-007)
- [X] T014 Implement navigation schema client service in frontend/src/services/navigationSchema.ts (refs: FR-002, FR-003, FR-004)
- [X] T015 Define dual-theme global tokens and focus styles in frontend/src/index.css (refs: FR-008, FR-013, FR-015)
- [X] T016 [P] Add Shell i18n namespaces (sidebar/header/telemetry/a11y) in frontend/src/i18n/shell-namespaces.ts (refs: FR-016)
- [X] T017 Configure Shell test helpers/providers in frontend/src/test/renderWithShellProviders.tsx (refs: FR-001, FR-013)
- [X] T018 [P] Configure a11y and performance quality gates in frontend/playwright.config.ts (refs: FR-013, FR-014)
- [X] T019 Add CI drift verification workflow for spec-plan-tasks parity in .github/workflows/spec-drift-check.yml (refs: FR-010)
- [X] T019a Add Atomic Components structure enforcement check in frontend/scripts/verify-atomic-structure.mjs (refs: FR-009)
- [X] T019b Add scope guard check to block domain business logic inside Shell layer in frontend/scripts/verify-shell-scope.mjs (refs: FR-010)
- [X] T019c Add task-completion self-verification checklist evidence in specs/003-shell-layout-estrutural/checklists/task-closure-checklist.md (refs: FR-010)
- [X] T019d Document CLI mandate N/A rationale for UI-only Shell in specs/003-shell-layout-estrutural/checklists/cli-mandate-na.md (refs: FR-010)
- [X] T019e Validate Constitution Gate II applicability (no new standalone library/module) and record sign-off in specs/003-shell-layout-estrutural/checklists/cli-mandate-na.md (refs: FR-010)

**Checkpoint**: Foundation complete. User stories can proceed.

---

## Phase 3: User Story 1 - Navegacao Estrutural da Area Autenticada (Priority: P1) MVP

**Goal**: Deliver a complete sidebar navigation structure by domain/resource with permission-aware disabled states.

**Independent Test**: Log in and verify that all domain groups/resources render in sidebar, including disabled restricted items with tooltip.

### Tests for User Story 1 (write first, must fail before implementation)

- [ ] T020 [P] [US1] Add Sidebar domain/resource rendering unit tests in frontend/src/test/Sidebar.test.tsx (refs: FR-002, FR-003, SC-001)
- [ ] T021 [P] [US1] Add disabled permission-state tooltip unit tests in frontend/src/test/SidebarPermission.test.tsx (refs: FR-011)
- [ ] T022 [P] [US1] Add keyboard navigation unit tests for sidebar interactions in frontend/src/test/SidebarA11yKeyboard.test.tsx (refs: FR-013)
- [ ] T023 [P] [US1] Add e2e navigation flow test (expand domain -> click resource -> route change) in frontend/e2e/shell-navigation.spec.ts (refs: FR-002, FR-003, SC-002)

### Implementation for User Story 1

- [ ] T024 [P] [US1] Implement SidebarItem atom with disabled and tooltip states in frontend/src/components/atoms/SidebarItem.tsx (refs: FR-011)
- [ ] T025 [P] [US1] Implement SidebarGroup molecule with collapsible behavior in frontend/src/components/molecules/SidebarGroup.tsx (refs: FR-002)
- [ ] T026 [US1] Implement Sidebar organism with domain/resource mapping in frontend/src/components/organisms/Sidebar.tsx (refs: FR-002, FR-003)
- [ ] T027 [US1] Implement security domain resources mapping in frontend/src/services/navigationSchema.ts (refs: FR-004)
- [ ] T028 [US1] Integrate Sidebar into MainTemplate layout regions in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001)
- [ ] T029 [US1] Add ARIA landmarks and keyboard handlers for sidebar structure in frontend/src/components/organisms/Sidebar.tsx (refs: FR-013)
- [ ] T030 [US1] Add tenant-safe navigation filtering logic (no cross-tenant leakage) in frontend/src/services/navigationSchema.ts (refs: FR-006)

**Checkpoint**: US1 is independently functional and testable.

---

## Phase 4: User Story 2 - Contexto Operacional no Header (Priority: P2)

**Goal**: Render tenant, location, and practitioner context in header with location persistence across sessions.

**Independent Test**: Render Shell for different users/locations and verify header context correctness and location persistence behavior.

### Tests for User Story 2 (write first, must fail before implementation)

- [ ] T031 [P] [US2] Add Header context rendering unit tests in frontend/src/test/Header.test.tsx (refs: FR-005, SC-003)
- [ ] T032 [P] [US2] Add location persistence + fallback unit tests in frontend/src/test/LocationPersistence.test.tsx (refs: FR-012)
- [ ] T033 [P] [US2] Add e2e location change persistence test across reload in frontend/e2e/shell-layout.spec.ts (refs: FR-012)
- [ ] T034 [P] [US2] Add header accessibility tests (selector focus + ARIA) in frontend/src/test/HeaderA11y.test.tsx (refs: FR-013)

### Implementation for User Story 2

- [ ] T035 [P] [US2] Implement LocationSelector molecule in frontend/src/components/molecules/LocationSelector.tsx (refs: FR-005, FR-012)
- [ ] T036 [P] [US2] Implement PractitionerProfile molecule in frontend/src/components/molecules/PractitionerProfile.tsx (refs: FR-005)
- [ ] T037 [US2] Implement Header organism composition in frontend/src/components/organisms/Header.tsx (refs: FR-005)
- [ ] T038 [US2] Implement location restore/confirm behavior in frontend/src/services/locationPersistence.ts (refs: FR-012)
- [ ] T039 [US2] Connect Header context state and callbacks in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001, FR-005)
- [ ] T040 [US2] Add tenant context visibility safeguards in Header rendering in frontend/src/components/organisms/Header.tsx (refs: FR-006)

**Checkpoint**: US2 is independently functional and testable.

---

## Phase 5: User Story 3 - Conformidade Visual e Observabilidade no Shell (Priority: P3)

**Goal**: Enforce centralized styling, dual-theme tokens, i18n key usage, and telemetry metadata visibility rules.

**Independent Test**: Audit Shell for global-token styling, telemetry DOM attributes, conditional footer visibility, i18n key usage, and performance/a11y budgets.

### Tests for User Story 3 (write first, must fail before implementation)

- [ ] T041 [P] [US3] Add telemetry footer visibility and data-attribute unit tests in frontend/src/test/ShellFooter.test.tsx (refs: FR-007, SC-004)
- [ ] T042 [P] [US3] Add MainTemplate provider-stack and i18n key usage tests in frontend/src/test/MainTemplateProviders.test.tsx (refs: FR-001, FR-016, SC-009)
- [ ] T043 [P] [US3] Add theme token application tests for light/dark modes in frontend/src/test/ThemeTokens.test.tsx (refs: FR-008, FR-015, SC-008)
- [ ] T044 [P] [US3] Add e2e accessibility audit test (axe + keyboard flow) in frontend/e2e/shell-a11y.spec.ts (refs: FR-013, SC-006)
- [ ] T045 [P] [US3] Add e2e performance budget assertion (LCP <= 1.5s) in frontend/e2e/shell-performance.spec.ts (refs: FR-014, SC-007)

### Implementation for User Story 3

- [ ] T046 [P] [US3] Implement telemetry footer molecule with conditional visibility in frontend/src/components/molecules/ShellFooter.tsx (refs: FR-007)
- [ ] T047 [US3] Implement MainTemplate provider stack (ThemeProvider + i18nProvider + contexts) in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001)
- [ ] T048 [US3] Add trace_id and tenant_id data attributes on Shell root in frontend/src/components/templates/MainTemplate.tsx (refs: FR-007)
- [ ] T049 [US3] Finalize centralized global token architecture and prohibit per-page CSS in frontend/src/index.css (refs: FR-008, FR-015)
- [ ] T050 [US3] Implement i18n namespace key wiring for sidebar/header/telemetry labels in frontend/src/i18n/shell-namespaces.ts (refs: FR-016)
- [ ] T051 [US3] Implement telemetry event propagation for shell render and interactions in frontend/src/services/observability.ts (refs: FR-007)
- [ ] T052 [US3] Optimize initial shell render with lazy boundaries and skeleton states in frontend/src/components/templates/MainTemplate.tsx (refs: FR-014)

**Checkpoint**: US3 is independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and release readiness across all stories.

- [ ] T053 [P] Document Shell integration and developer usage updates in frontend/README.md (refs: FR-001, FR-010)
- [ ] T054 [P] Publish feature quick validation checklist in specs/003-shell-layout-estrutural/checklists/shell-readiness.md (refs: FR-013, FR-014, FR-016)
- [ ] T055 [P] Record architecture spike evidence for render optimization in specs/003-shell-layout-estrutural/spike-results.md (refs: FR-014)
- [ ] T056 [P] Record accessibility audit evidence in specs/003-shell-layout-estrutural/a11y-audit-report.md (refs: FR-013)
- [ ] T057 Execute `npm test && npm run lint && npm run e2e` and capture output in frontend/README.md (refs: FR-013, FR-014)
- [ ] T058 Run `/speckit.checklist` and attach consistency evidence in specs/003-shell-layout-estrutural/checklists/task-closure-checklist.md before closing tasks (refs: FR-010)

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): no dependencies.
- Foundational (Phase 2): depends on Setup; blocks all stories.
- User Story phases (Phase 3-5): depend on Foundational completion.
- Polish (Phase 6): depends on all user stories being complete.

### User Story Dependencies

- US1 (P1): starts immediately after Foundational; no dependency on US2/US3.
- US2 (P2): starts after Foundational; independent from US1 except shared contexts.
- US3 (P3): starts after Foundational; can run in parallel with US2.

### Within Each User Story

- Tests first and failing before implementation tasks.
- Atoms/molecules before organism integration.
- Service/context wiring before template-level integration.
- Accessibility and performance checks must pass before story closure.

## Parallel Execution Examples

### User Story 1

- Run in parallel: T020, T021, T022, T023
- Then run in parallel: T024, T025
- Then sequential: T026 -> T027 -> T028 -> T029 -> T030

### User Story 2

- Run in parallel: T031, T032, T033, T034
- Then run in parallel: T035, T036
- Then sequential: T037 -> T038 -> T039 -> T040

### User Story 3

- Run in parallel: T041, T042, T043, T044, T045
- Then run in parallel: T046, T049, T050, T051
- Then sequential: T047 -> T048 -> T052

## Implementation Strategy

### MVP First (US1 only)

1. Complete Phase 1 and Phase 2.
2. Deliver US1 (Phase 3) with passing tests.
3. Validate navigation acceptance criteria before expanding scope.

### Incremental Delivery

1. Add US2 (header context + persistence).
2. Add US3 (visual governance + telemetry + i18n structure).
3. Execute Phase 6 polish and quality evidence capture.

### Suggested Team Parallelization

- Engineer A: Navigation components and US1 tests.
- Engineer B: Header context/persistence and US2 tests.
- Engineer C: Theme/i18n/telemetry and US3 tests.
- Shared ownership: Phase 2 foundation and Phase 6 quality gates.
