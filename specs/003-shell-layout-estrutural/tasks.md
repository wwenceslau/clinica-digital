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

- [X] T020 [P] [US1] Add Sidebar domain/resource rendering unit tests in frontend/src/test/Sidebar.test.tsx (refs: FR-002, FR-003, SC-001)
- [X] T021 [P] [US1] Add disabled permission-state tooltip unit tests in frontend/src/test/SidebarPermission.test.tsx (refs: FR-011)
- [X] T022 [P] [US1] Add keyboard navigation unit tests for sidebar interactions in frontend/src/test/SidebarA11yKeyboard.test.tsx (refs: FR-013)
- [X] T023 [P] [US1] Add e2e navigation flow test (expand domain -> click resource -> route change) in frontend/e2e/shell-navigation.spec.ts (refs: FR-002, FR-003, SC-002)

### Implementation for User Story 1

- [X] T024 [P] [US1] Implement SidebarItem atom with disabled and tooltip states in frontend/src/components/atoms/SidebarItem.tsx (refs: FR-011)
- [X] T025 [P] [US1] Implement SidebarGroup molecule with collapsible behavior in frontend/src/components/molecules/SidebarGroup.tsx (refs: FR-002)
- [X] T026 [US1] Implement Sidebar organism with domain/resource mapping in frontend/src/components/organisms/Sidebar.tsx (refs: FR-002, FR-003)
- [X] T027 [US1] Implement security domain resources mapping in frontend/src/services/navigationSchema.ts (refs: FR-004)
- [X] T028 [US1] Integrate Sidebar into MainTemplate layout regions in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001)
- [X] T029 [US1] Add ARIA landmarks and keyboard handlers for sidebar structure in frontend/src/components/organisms/Sidebar.tsx (refs: FR-013)
- [X] T030 [US1] Add tenant-safe navigation filtering logic (no cross-tenant leakage) in frontend/src/services/navigationSchema.ts (refs: FR-006)

**Checkpoint**: US1 is independently functional and testable.

---

## Phase 4: User Story 2 - Contexto Operacional no Header (Priority: P2)

**Goal**: Render tenant, location, and practitioner context in header with location persistence across sessions.

**Independent Test**: Render Shell for different users/locations and verify header context correctness and location persistence behavior.

---

### Tests for User Story 2 — WRITE FIRST, MUST FAIL BEFORE IMPLEMENTATION

> **TDD Gate**: Tests T031–T034 must be committed and confirmed failing (for components not yet created)
> before any of T035–T040 implementation tasks begin.

- [X] T031 [P] [US2] Add Header context rendering unit tests in frontend/src/test/Header.test.tsx (refs: FR-005, SC-003)
  **DoD**: file exists at `frontend/src/test/Header.test.tsx` + `npx vitest run Header.test` exits 0 covering: (a) renders tenant_name, (b) renders active_location_name, (c) renders practitioner_name + role, (d) location selector opens dropdown + `tsc --noEmit` exits 0
  **Depends on**: T017 (renderWithShellProviders), T009 (ThemeContext), T011 (ShellContext) — test file can be written even before Header.tsx exists (import will fail = red phase)

- [X] T032 [P] [US2] Add location persistence + fallback unit tests in frontend/src/test/LocationPersistence.test.tsx (refs: FR-012)
  **DoD**: file exists at `frontend/src/test/LocationPersistence.test.tsx` + `npx vitest run LocationPersistence.test` exits 0 covering: (a) `resolveActiveLocation` returns first when nothing persisted, (b) returns persisted match when found, (c) falls back to first when persisted ID not in available list, (d) returns null when available list is empty + `tsc --noEmit` exits 0
  **Depends on**: T012 (locationPersistence.ts — service already exists; tests validate its behavior)

- [X] T033 [P] [US2] Add e2e location change persistence test across reload in frontend/e2e/shell-layout.spec.ts (refs: FR-012)
  **DoD**: file exists at `frontend/e2e/shell-layout.spec.ts` + `npx playwright test e2e/shell-layout.spec.ts --reporter=line` exits 0 covering: (a) user selects location B, (b) page reloads, (c) location B is still active in selector
  **Depends on**: T039 (Header integrated into MainTemplate with locationPersistence callbacks), T035 (LocationSelector)

- [X] T034 [P] [US2] Add header accessibility tests (selector focus + ARIA) in frontend/src/test/HeaderA11y.test.tsx (refs: FR-013)
  **DoD**: file exists at `frontend/src/test/HeaderA11y.test.tsx` + `npx vitest run HeaderA11y.test` exits 0 covering: (a) LocationSelector has `role="combobox"` or `role="listbox"`, (b) PractitionerProfile has `aria-label` with practitioner name, (c) focus moves to first option on selector open + `tsc --noEmit` exits 0
  **Depends on**: T017 (renderWithShellProviders) — test file can be committed failing before T035/T036/T037 exist

---

### Implementation for User Story 2

- [X] T035 [P] [US2] Implement LocationSelector molecule in frontend/src/components/molecules/LocationSelector.tsx (refs: FR-005, FR-012)
  **DoD**: file exists at declared path + `npx vitest run LocationPersistence.test` passes + `npx vitest run Header.test` passes (selector renders and calls onSelect) + `npx vitest run HeaderA11y.test` passes for selector ARIA + `grep -rn "LocationSelector" frontend/src/components/organisms/Header.tsx` returns ≥1 match + `tsc --noEmit` exits 0
  **Depends on**: T031 (Header.test.tsx committed as failing), T032 (LocationPersistence.test.tsx passing), T034 (HeaderA11y.test.tsx committed as failing), T012 (locationPersistence service)

- [X] T036 [P] [US2] Implement PractitionerProfile molecule in frontend/src/components/molecules/PractitionerProfile.tsx (refs: FR-005)
  **DoD**: file exists at declared path + `npx vitest run Header.test` passes for practitioner name + role rendering + `npx vitest run HeaderA11y.test` passes for practitioner aria-label + `grep -rn "PractitionerProfile" frontend/src/components/organisms/Header.tsx` returns ≥1 match + `tsc --noEmit` exits 0
  **Depends on**: T031 (Header.test.tsx committed), T034 (HeaderA11y.test.tsx committed), T007 (shell.types.ts — HeaderContextData interface)

- [X] T037 [US2] Implement Header organism composition in frontend/src/components/organisms/Header.tsx (refs: FR-005)
  **DoD**: file exists at `frontend/src/components/organisms/Header.tsx` + `npx vitest run Header.test` exits 0 (all tests pass) + `npx vitest run HeaderA11y.test` exits 0 + `grep -rn "LocationSelector\|PractitionerProfile" frontend/src/components/organisms/Header.tsx` returns ≥2 matches + `tsc --noEmit` exits 0
  **Depends on**: T035 (LocationSelector), T036 (PractitionerProfile), T031 (tests must go green)

- [X] T038 [US2] Implement location restore/confirm behavior wiring in frontend/src/services/locationPersistence.ts (refs: FR-012)
  **DoD**: `grep -rn "from.*locationPersistence" frontend/src/ | grep -v locationPersistence.ts` returns ≥1 match (service is imported by a consumer) + `npx vitest run LocationPersistence.test` exits 0 (all edge cases pass) + `tsc --noEmit` exits 0
  **Depends on**: T032 (LocationPersistence.test.tsx passing), T035 (LocationSelector imports the service)

- [X] T039 [US2] Connect Header context state and callbacks in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001, FR-005)
  **DoD**: `grep -rn "Header" frontend/src/components/templates/MainTemplate.tsx` returns ≥1 import + `grep -rn "locationPersistence\|resolveActiveLocation" frontend/src/components/templates/MainTemplate.tsx` returns ≥1 match + `npx vitest run Header.test` passes + `tsc --noEmit` exits 0
  **Depends on**: T037 (Header organism complete), T038 (locationPersistence wired)

- [X] T040 [US2] Add tenant context visibility safeguards in Header rendering in frontend/src/components/organisms/Header.tsx (refs: FR-006)
  **DoD**: `grep -rn "tenant_id\|tenantId" frontend/src/components/organisms/Header.tsx` returns ≥1 defensive check + `npx vitest run Header.test` passes (including a test that verifies only current-tenant data is shown) + `tsc --noEmit` exits 0
  **Depends on**: T039 (Header integrated into MainTemplate, tenant context flows in), T031 (Header.test.tsx — add tenant safeguard test case)

**Checkpoint**: US2 is independently functional. Run: `npx vitest run Header.test LocationPersistence.test HeaderA11y.test` and confirm all pass before proceeding.

---

## Phase 5: User Story 3 - Conformidade Visual e Observabilidade no Shell (Priority: P3)

**Goal**: Enforce centralized styling, dual-theme tokens, i18n key usage, and telemetry metadata visibility rules.

**Independent Test**: Audit Shell for global-token styling, telemetry DOM attributes, conditional footer visibility, i18n key usage, and performance/a11y budgets.

---

### Tests for User Story 3 — WRITE FIRST, MUST FAIL BEFORE IMPLEMENTATION

- [X] T041 [P] [US3] Add telemetry footer visibility and data-attribute unit tests in frontend/src/test/ShellFooter.test.tsx (refs: FR-007, SC-004)
  **DoD**: file exists at `frontend/src/test/ShellFooter.test.tsx` + `npx vitest run ShellFooter.test` exits 0 covering: (a) renders `data-trace-id` attr on root, (b) renders `data-tenant-id` attr on root, (c) footer hidden when `debug_mode=false`, (d) footer visible when `debug_mode=true` + `tsc --noEmit` exits 0
  **Depends on**: T013 (observability.ts — getDomTelemetryAttributes), T017 (renderWithShellProviders)

- [X] T042 [P] [US3] Add MainTemplate provider-stack and i18n key usage tests in frontend/src/test/MainTemplateProviders.test.tsx (refs: FR-001, FR-016, SC-009)
  **DoD**: file exists at `frontend/src/test/MainTemplateProviders.test.tsx` + `npx vitest run MainTemplateProviders.test` exits 0 covering: (a) ThemeContextProvider present in tree, (b) LocaleContextProvider present, (c) ShellContextProvider present, (d) at least one i18n key from `headerNamespaceKeys` resolves to non-empty string + `tsc --noEmit` exits 0
  **Depends on**: T009 (ThemeContext), T010 (LocaleContext), T011 (ShellContext), T016 (shell-namespaces.ts), T017 (renderWithShellProviders)

- [X] T043 [P] [US3] Add theme token application tests for light/dark modes in frontend/src/test/ThemeTokens.test.tsx (refs: FR-008, FR-015, SC-008)
  **DoD**: file exists at `frontend/src/test/ThemeTokens.test.tsx` + `npx vitest run ThemeTokens.test` exits 0 covering: (a) `--primary` CSS custom property defined in `:root`, (b) `--background` defined, (c) switching to dark mode applies different `--background` value + `tsc --noEmit` exits 0
  **Depends on**: T015 (index.css — CSS tokens exist), T009 (ThemeContext — toggle mechanism)

- [X] T044 [P] [US3] Add e2e accessibility audit test (axe + keyboard flow) in frontend/e2e/shell-a11y.spec.ts (refs: FR-013, SC-006)
  **DoD**: file exists at `frontend/e2e/shell-a11y.spec.ts` + `npx playwright test e2e/shell-a11y.spec.ts --reporter=line` exits 0 with 0 WCAG 2.1 AA violations reported by axe-core
  **Depends on**: T039 (Header integrated), T040 (tenant safeguards), T047 (MainTemplate provider stack complete)

- [X] T045 [P] [US3] Add e2e performance budget assertion (LCP ≤ 1.5s) in frontend/e2e/shell-performance.spec.ts (refs: FR-014, SC-007)
  **DoD**: file exists at `frontend/e2e/shell-performance.spec.ts` + `npx playwright test e2e/shell-performance.spec.ts --reporter=line` exits 0 with LCP ≤ 1500ms assertion passing
  **Depends on**: T052 (lazy boundaries and skeleton states implemented)

---

### Implementation for User Story 3

- [X] T046 [P] [US3] Implement telemetry footer molecule with conditional visibility in frontend/src/components/molecules/ShellFooter.tsx (refs: FR-007)
  **DoD**: file exists at `frontend/src/components/molecules/ShellFooter.tsx` + `npx vitest run ShellFooter.test` exits 0 + `grep -rn "getDomTelemetryAttributes\|data-trace-id" frontend/src/components/molecules/ShellFooter.tsx` returns ≥1 match + `tsc --noEmit` exits 0
  **Depends on**: T041 (ShellFooter.test.tsx committed — must go green), T013 (observability.ts — getDomTelemetryAttributes)

- [X] T047 [US3] Implement MainTemplate provider stack (ThemeProvider + i18nProvider + contexts) in frontend/src/components/templates/MainTemplate.tsx (refs: FR-001)
  **DoD**: `grep -rn "ThemeContextProvider\|LocaleContextProvider\|ShellContextProvider" frontend/src/components/templates/MainTemplate.tsx` returns ≥3 distinct matches + `npx vitest run MainTemplateProviders.test` exits 0 + `tsc --noEmit` exits 0
  **Depends on**: T042 (MainTemplateProviders.test.tsx committed — must go green), T039 (Header already connected in MainTemplate)

- [X] T048 [US3] Add trace_id and tenant_id data attributes on Shell root in frontend/src/components/templates/MainTemplate.tsx (refs: FR-007)
  **DoD**: `grep -rn "data-trace-id\|data-tenant-id" frontend/src/components/templates/MainTemplate.tsx` returns ≥2 matches + `npx vitest run ShellFooter.test` passes (covers data attrs) + `npx vitest run MainTemplateProviders.test` passes + `tsc --noEmit` exits 0
  **Depends on**: T013 (getDomTelemetryAttributes), T047 (MainTemplate provider stack)

- [X] T049 [US3] Finalize centralized global token architecture and prohibit per-page CSS in frontend/src/index.css (refs: FR-008, FR-015)
  **DoD**: `grep -c "var(--" frontend/src/index.css` returns ≥10 CSS custom properties + `npx vitest run ThemeTokens.test` exits 0 + `grep -rn "style={{" frontend/src/components/ | grep -v ".test." | wc -l` documents baseline for per-component inline styles audit + `tsc --noEmit` exits 0
  **Depends on**: T043 (ThemeTokens.test.tsx — must go green), T015 (index.css tokens exist)

- [X] T050 [US3] Implement i18n namespace key wiring for sidebar/header/telemetry labels in frontend/src/i18n/ (refs: FR-016)
  **DoD**: `grep -rn "from.*shell-namespaces" frontend/src/ | grep -v shell-namespaces.ts` returns ≥1 match (keys are imported by at least one component) + `npx vitest run MainTemplateProviders.test` passes for i18n key resolution + `tsc --noEmit` exits 0
  **Depends on**: T016 (shell-namespaces.ts keys defined), T042 (MainTemplateProviders.test.tsx — must go green)

- [X] T051 [US3] Implement telemetry event propagation for shell render and interactions in frontend/src/services/observability.ts (refs: FR-007)
  **DoD**: `grep -rn "emitShellTelemetry" frontend/src/ | grep -v observability.ts` returns ≥2 matches (called on shell.render + shell.location.changed events) + `npx vitest run ShellFooter.test` passes + `tsc --noEmit` exits 0
  **Depends on**: T013 (observability.ts — emitShellTelemetry defined), T041 (ShellFooter.test.tsx covers telemetry calls), T046 (ShellFooter), T039 (Header — location change hook)

- [X] T052 [US3] Optimize initial shell render with lazy boundaries and skeleton states in frontend/src/components/templates/MainTemplate.tsx (refs: FR-014)
  **DoD**: `grep -rn "React.lazy\|Suspense" frontend/src/components/templates/MainTemplate.tsx` returns ≥1 match + `npx vitest run MainTemplateProviders.test` passes + `tsc --noEmit` exits 0 + T045 E2E performance test passes (LCP ≤ 1500ms)
  **Depends on**: T047 (MainTemplate provider stack complete), T045 (E2E performance test written)

**Checkpoint**: US3 is independently functional. Run: `npx vitest run ShellFooter.test MainTemplateProviders.test ThemeTokens.test` and confirm all pass before proceeding.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and release readiness across all stories.

- [X] T053 [P] Document Shell integration and developer usage updates in frontend/README.md (refs: FR-001, FR-010)
  **DoD**: `grep -rn "LocationSelector\|Header\|ShellFooter" frontend/README.md` returns ≥3 matches documenting usage + file exists
  **Depends on**: T052 (implementation complete)

- [X] T054 [P] Publish feature quick validation checklist in specs/003-shell-layout-estrutural/checklists/shell-readiness.md (refs: FR-013, FR-014, FR-016)
  **DoD**: file exists + contains ≥10 checkable items covering a11y, performance, i18n, and telemetry + all items marked `[x]`
  **Depends on**: T057 (all tests passing)

- [X] T055 [P] Record architecture spike evidence for render optimization in specs/003-shell-layout-estrutural/spike-results.md (refs: FR-014)
  **DoD**: file exists + documents chosen lazy-loading strategy + trade-offs + performance measurements from T045
  **Depends on**: T045 (performance E2E passing), T052 (lazy boundaries implemented)

- [X] T056 [P] Record accessibility audit evidence in specs/003-shell-layout-estrutural/a11y-audit-report.md (refs: FR-013)
  **DoD**: file exists + contains axe-core output from T044 + documents 0 WCAG 2.1 AA violations
  **Depends on**: T044 (E2E a11y test passing)

- [X] T057 Execute `npm test && npm run lint && npx playwright test` and capture output evidence (refs: FR-013, FR-014)
  **DoD**: all three commands exit 0 + output captured in specs/003-shell-layout-estrutural/checklists/final-validation-evidence.md
  **Depends on**: T044, T045, T052 (all implementation and E2E complete)

- [X] T058 Run consistency verification and attach evidence in specs/003-shell-layout-estrutural/checklists/task-closure-checklist.md (refs: FR-010)
  **DoD**: task-closure-checklist.md updated with evidence entry for every task T031–T058 + all entries marked `[x]` + `tsc --noEmit` exits 0
  **Depends on**: T057 (final validation passing)

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
