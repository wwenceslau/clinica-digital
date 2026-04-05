# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or [NEEDS CLARIFICATION]]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or [NEEDS CLARIFICATION]]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or [NEEDS CLARIFICATION]]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or [NEEDS CLARIFICATION]]
**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or [NEEDS CLARIFICATION]]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or [NEEDS CLARIFICATION]]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or [NEEDS CLARIFICATION]]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or [NEEDS CLARIFICATION]]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

> Source of truth: `.specify/memory/constitution.md` v1.6.0

| # | Gate | Status |
|---|------|--------|
| 0 | **Multi-Tenant**: Every new entity has a `tenant_id` column and a PostgreSQL RLS policy identified. | ☐ |
| I | **Test-First**: Test plan present with contract, integration, and unit test categories defined. | ☐ |
| II | **CLI Mandate**: Each new library/module exposes primary flow via CLI with JSON-capable output contract. | ☐ |
| III | **Library-First**: Library module boundary named; public API contract (interface/DTO/OpenAPI) identified. | ☐ |
| IV | **Spec-Truth & Traceability**: Every plan decision links to one or more requirement IDs in `spec.md`. | ☐ |
| V | **Uncertainty Control**: Ambiguities are explicitly marked `[NEEDS CLARIFICATION]` and resolved before implementation. | ☐ |
| VI | **Data Security & Errors**: New PHI/PII fields classified and encrypted strategy noted; API errors mapped to FHIR `OperationOutcome`. | ☐ |
| VII | **Regulatory**: RNDS/LGPD impact assessed; compliance review flagged (`YES / NO / N/A`) with rationale. | ☐ |
| VIII | **Simplicity**: Any extra abstraction is justified in `Complexity Tracking` with rejected simpler alternatives. | ☐ |
| IX | **Drift Governance**: CI/CD includes drift verification (e.g., `spec-kit-verify-tasks`) and blocks merge on divergence. | ☐ |
| X | **Performance Isolation**: Tenant-based rate limiting and quotas specified for each new API/CLI entrypoint. | ☐ |
| XI | **Observability Contract**: Monitoring plan defines tracing + metrics mapping, `trace_id` propagation, and tenant-context JSON logging. | ☐ |
| XII | **Agent Governance**: Plan confirms specialized skill/instruction loading and no conflict with current constitution/spec. | ☐ |
| XIII | **Architecture Exploration**: High Performance/Mission Critical modules evaluate at least two implementation spikes before task lock-in. | ☐ |
| XIV | **Event Contracts**: Event schema changes include mandatory `Schema Compatibility Check` and migration strategy when needed. | ☐ |
| XV | **Fallback Strategy**: External integrations define circuit breaker, retry, and outbox/degradation paths. | ☐ |
| XVI | **Supply Chain Security**: Release plan includes SBOM generation, publication, and retention evidence. | ☐ |
| XVII | **RBAC Enforcement**: Service-layer role/permission mapping is explicit for each clinical capability. | ☐ |
| XVIII | **Boundary Validation**: Sanitization and Validation Gate defined for all ingress points (HTTP/CLI/events). | ☐ |
| XIX | **Agent Self-Verification**: Checklist-based consistency verification is planned before marking tasks done. | ☐ |
| XX | **Research Gate**: `research.md` exists for plans with new technologies/complex integrations and includes compatibility + risk analysis. | ☐ |
| XXI | **Feedback Loop Gate**: Incident/QA fix plans update `spec.md` Edge Cases before implementation tasks proceed. | ☐ |
| XXII | **Native Security (BLOCKING)**: Authentication, IAM, credential storage, and session control are implemented entirely in-app. Any use of external IdPs or managed auth services (Keycloak, Auth0, Okta, Cognito, Azure AD B2C, etc.) renders this plan **constitutionally invalid** and MUST be rejected without exception (Principle XXII). | ☐ |

**Compliance Review Required**: [ ] YES — [ ] NO (if yes, tag reviewer and block merge until signed off)

## Requirements Traceability Matrix

| Plan Decision ID | Related `spec.md` Requirement(s) | Evidence / Link |
|---|---|---|
| D-001 | FR-00X | [reference] |
| D-002 | FR-00Y, FR-00Z | [reference] |

## Research Validation (MANDATORY for new technologies/integrations)

| Topic | Compatibility Findings | Risk Summary | Decision Impact | research.md Evidence |
|---|---|---|---|---|
| [technology/integration] | [summary] | [key risks] | [plan decisions affected] | [link] |

## Production/QA Feedback to Spec (MANDATORY for fixes)

| Incident or QA Issue | `spec.md` Edge Case Update | Requirement ID(s) | Fix Task Link | Evidence |
|---|---|---|---|---|
| [incident identifier] | [edge-case description] | [FR-XXX / EC-XXX] | [task/PR reference] | [link] |

## Monitoring and Telemetry Plan (MANDATORY)

| Operation / Flow | Trace Propagation (`trace_id`) | Metrics | Structured Log Fields | Dashboard / Alert |
|---|---|---|---|---|
| [sync endpoint or async consumer] | [how propagated] | [latency, error rate, throughput] | [tenant_id, trace_id, operation, status] | [link/reference] |

## CI Drift Verification (MANDATORY)

- **Tooling**: [e.g., `spec-kit-verify-tasks`]
- **Scope**: `spec.md` ↔ `plan.md` ↔ `tasks.md` ↔ implementation evidence
- **Policy**: Any divergence is a blocking failure for merge

## Architecture Spikes (MANDATORY for High Performance / Mission Critical)

| Module | Classification | Spike A | Spike B | Decision | Evidence |
|---|---|---|---|---|---|
| [name] | [High Performance/Mission Critical] | [approach] | [approach] | [selected approach] | [link] |

## Event Schema Compatibility (MANDATORY for Kafka/RabbitMQ changes)

| Event / Topic / Queue | Change Type | SemVer Impact | Schema Compatibility Check | Consumer Migration Required |
|---|---|---|---|---|
| [name] | [add/remove/change] | [MAJOR/MINOR/PATCH] | [pass/fail + evidence] | [YES/NO + plan link] |

## Fallback Strategy (MANDATORY for external integrations)

| Integration | Circuit Breaker | Retry Policy | Outbox/Buffer Strategy | Clinical Safety Impact |
|---|---|---|---|---|
| [RNDS/KMS/Vault/etc.] | [pattern/config] | [policy] | [approach] | [safe degradation behavior] |

## SBOM and Supply Chain Evidence (MANDATORY for releases)

| Artifact | SBOM Format | Generation Step | Storage Location | Verification Step |
|---|---|---|---|---|
| [library/module] | [CycloneDX/SPDX] | [pipeline step] | [artifact registry] | [validation tool/check] |

## RBAC Mapping Matrix (MANDATORY)

| Service Operation | Clinical Role(s) | Permission Key | Enforcement Point | Audit Evidence |
|---|---|---|---|---|
| [operation] | [role list] | [perm.key] | [service method/policy] | [log/report] |

## Design System Conformance (MANDATORY for UI changes)

| UI Capability | Design System Component(s) | Token Set | Accessibility Check | Evidence |
|---|---|---|---|---|
| [screen/flow] | [MUI/Tailwind components] | [spacing/color/typography tokens] | [WCAG criteria] | [link/report] |

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
