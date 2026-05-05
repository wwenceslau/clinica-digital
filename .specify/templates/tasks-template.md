---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY. For every story, write tests first and ensure they fail before implementation (Test-First).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Constitution Validation Checkpoints (MANDATORY)

- Every user-story task set MUST include test tasks before implementation tasks.
- Every new module/library MUST include at least one CLI contract and CLI test task.
- Every implementation task MUST reference one or more `spec.md` requirement IDs.
- Any ambiguity in task text MUST include `[NEEDS CLARIFICATION]` and block implementation.
- Any non-trivial abstraction task MUST include a paired `Complexity Tracking` justification task.
- API error-handling tasks MUST enforce FHIR `OperationOutcome`.
- Audit-related tasks MUST preserve append-only behavior (no UPDATE/DELETE for service accounts).
- Any new API endpoint MUST include an explicit task named `Verificacao de Quotas`.
- Task set MUST include observability tasks for trace propagation and tenant-context JSON logs.
- Task set MUST include drift-verification integration in CI/CD before merge.
- For Kafka/RabbitMQ changes, task set MUST include explicit `Schema Compatibility Check`.
- For external integrations, task set MUST include explicit `Fallback Strategy` implementation task.
- Task set MUST include RBAC mapping and service-layer permission enforcement tasks.
- Task set MUST include Sanitization and Validation Gate tasks for each ingress boundary.
- Before marking any task complete, agent MUST execute checklist-based self-verification.
- Every task MUST include a machine-verifiable **DoD** field with at least one command (tsc --noEmit,
  npx vitest run <test>, grep -rn "import.*<Artefato>" src/, npx playwright test <spec>). Tasks
  without a verifiable DoD field are INVALID and MUST be corrected before the plan is approved.
  (Principle XXIII — Task Definition of Done Mandate).
- Every non-setup task MUST include a **Depends on** field listing prerequisite task IDs. Omitting
  dependencies is only allowed for genuinely parallel Phase 1 setup tasks (Principle XXIII).
- Complex technology/integration efforts MUST include `research.md` validation tasks.
- Incident/QA fixes MUST include `spec.md` Edge Cases update tasks before code fixes.
- Authentication and session management MUST be implemented entirely in-app; any task
  introducing an external IdP, managed auth service, or third-party auth library
  (Keycloak, Auth0, Okta, Cognito, Azure AD B2C, etc.) violates Principle XXII
  and MUST be removed or blocked before the task set is approved.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

### Mandatory Task Block Format (Principle XXIII)

Every task MUST use this extended format (two sub-lines after the checkbox):

```markdown
- [ ] T014 [US1] Implement LocationSelector molecule in frontend/src/components/molecules/LocationSelector.tsx (refs: FR-005)
  **DoD**: file exists at declared path + `npx vitest run LocationPersistence.test` passes + `grep -rn LocationSelector frontend/src/components/organisms/Header.tsx` returns ≥1 match + `tsc --noEmit` exits 0
  **Depends on**: T031 (Header.test.tsx written and failing), T032 (LocationPersistence.test.tsx written and failing), T012 (locationPersistence.ts service)
```

Tasks missing **DoD** or **Depends on** are INVALID and block the task set from being approved.

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Setup database schema and migrations framework
- [ ] T005 [P] Implement authentication/authorization framework
- [ ] T006 [P] Setup API routing and middleware structure
- [ ] T007 Create base models/entities that all stories depend on
- [ ] T008 Configure error handling and logging infrastructure
- [ ] T009 Setup environment configuration management
- [ ] T009a Configure CI drift verification (e.g., `spec-kit-verify-tasks`) as merge blocker
- [ ] T009b Configure SBOM generation in CI/CD for release artifacts
- [ ] T009c Define centralized RBAC policy map (roles, permissions, clinical scopes)
- [ ] T009d Define shared Sanitization and Validation Gate for ingress adapters
- [ ] T009e Validate `research.md` is present for new technologies/complex integrations

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Contract test for [endpoint] in tests/contract/test_[name].py (refs: FR-XXX)
- [ ] T011 [P] [US1] Integration test for [user journey] in tests/integration/test_[name].py (refs: FR-XXX)
- [ ] T011a [P] [US1] CLI contract test for [library command] in tests/contract/test_cli_[name].py (refs: FR-XXX)

### Implementation for User Story 1

- [ ] T012 [P] [US1] Create [Entity1] model in src/models/[entity1].py
- [ ] T013 [P] [US1] Create [Entity2] model in src/models/[entity2].py
- [ ] T014 [US1] Implement [Service] in src/services/[service].py (depends on T012, T013)
- [ ] T015 [US1] Implement [endpoint/feature] in src/[location]/[file].py (refs: FR-XXX)
- [ ] T016 [US1] Add validation and FHIR OperationOutcome error handling (refs: FR-XXX)
- [ ] T017 [US1] Add tracing + tenant-context JSON logging for user story 1 operations (refs: FR-XXX)
- [ ] T017a [US1] Verificacao de Quotas for new API endpoint(s) based on tenant_id (refs: FR-XXX)
- [ ] T017b [US1] Schema Compatibility Check for Kafka/RabbitMQ message changes (refs: FR-XXX)
- [ ] T017c [US1] Implement Fallback Strategy (circuit breaker + retry + outbox when applicable) for external integrations (refs: FR-XXX)
- [ ] T017d [US1] Implement service-layer RBAC checks for clinical permissions (refs: FR-XXX)
- [ ] T017e [US1] Apply Sanitization and Validation Gate to inbound payloads (FHIR/DTO) (refs: FR-XXX)
- [ ] T017f [US1] Run /speckit.checklist consistency verification before closing US1 tasks (refs: FR-XXX)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY) ⚠️

- [ ] T018 [P] [US2] Contract test for [endpoint] in tests/contract/test_[name].py (refs: FR-XXX)
- [ ] T019 [P] [US2] Integration test for [user journey] in tests/integration/test_[name].py (refs: FR-XXX)

### Implementation for User Story 2

- [ ] T020 [P] [US2] Create [Entity] model in src/models/[entity].py
- [ ] T021 [US2] Implement [Service] in src/services/[service].py
- [ ] T022 [US2] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T023 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY) ⚠️

- [ ] T024 [P] [US3] Contract test for [endpoint] in tests/contract/test_[name].py (refs: FR-XXX)
- [ ] T025 [P] [US3] Integration test for [user journey] in tests/integration/test_[name].py (refs: FR-XXX)

### Implementation for User Story 3

- [ ] T026 [P] [US3] Create [Entity] model in src/models/[entity].py
- [ ] T027 [US3] Implement [Service] in src/services/[service].py
- [ ] T028 [US3] Implement [endpoint/feature] in src/[location]/[file].py

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit tests (if requested) in tests/unit/
- [ ] TXXX Security hardening
- [ ] TXXX Run Schema Compatibility Check reports and attach evidence
- [ ] TXXX Validate Fallback Strategy behavior under dependency failure scenarios
- [ ] TXXX Validate RBAC coverage report and permission audit logs
- [ ] TXXX Validate boundary sanitization/validation rejection paths
- [ ] TXXX Execute /speckit.checklist before final task closure
- [ ] TXXX Confirm SBOM is configured and generated for each new module introduced
- [ ] TXXX Validate incident/QA fixes updated `spec.md` Edge Cases before code changes
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- CLI contract and CLI tests MUST exist for new libraries/modules
- Each implementation task MUST reference requirement IDs from `spec.md`
- Ambiguous tasks MUST be marked `[NEEDS CLARIFICATION]` and resolved first
- Every new API endpoint MUST have an explicit `Verificacao de Quotas` task
- Observability tasks MUST include `trace_id` propagation and tenant-context logs
- Event-driven changes MUST include `Schema Compatibility Check`
- External integrations MUST include `Fallback Strategy` task
- Service-layer RBAC mapping MUST exist for each exposed clinical capability
- Boundary Sanitization and Validation Gate MUST exist for each ingress channel
- New technology/integration work MUST include a `research.md` validation task
- Incident/QA fixes MUST update `spec.md` Edge Cases before fix implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all mandatory tests for User Story 1 together:
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Add `(refs: FR-XXX)` to implementation tasks for spec traceability
- Add explicit `Verificacao de Quotas` task for each new endpoint introduced
- Add explicit `Schema Compatibility Check` for Kafka/RabbitMQ contract updates
- Add explicit `Fallback Strategy` task for external dependency integrations
- Add explicit RBAC service-permission mapping tasks
- Add explicit Sanitization and Validation Gate tasks for data ingress
- Run checklist-based self-verification before marking tasks as completed
- Add final checkpoint verifying SBOM setup/generation for each new module
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
