# Constitutional Verification Evidence

Date: 2026-04-07
Scope: US1 (tenant isolation foundation) and US2 (module boundaries + CLI)
Task reference: T165, T083, T084

## Objective
Provide auditable evidence that US1 satisfies constitutional gates used for progression to US2, with focus on Art. 0, Art. I, Art. II and Art. XI.

## Evidence Summary

### Art. 0 - Tenant isolation and fail-closed boundary
- Contract test passed: `TenantBoundaryContractTest`.
- Integration test passed: `TenantIsolationRLSTest`.
- Integration test passed: `TenantMissingContextTest`.
- Confirmed fail-closed behavior on missing tenant context and RLS isolation at persistence layer.

Evidence files:
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.contract.TenantBoundaryContractTest.xml`
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.integration.TenantIsolationRLSTest.xml`
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.integration.TenantMissingContextTest.xml`

### Art. I - Test-first and auditable validation
- Contract test passed: `DevProfileTemplateContractTest`.
- All US1 gateway verification tests executed with zero failures and zero errors.
- Surefire XML reports provide objective run counts and timing, suitable for audit trail.

Evidence files:
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.contract.DevProfileTemplateContractTest.xml`
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.contract.TenantBoundaryContractTest.xml`
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.integration.TenantIsolationRLSTest.xml`
- `backend/clinic-gateway-app/target/surefire-reports/TEST-com.clinicadigital.gateway.integration.TenantMissingContextTest.xml`

### Art. II - CLI/module boundary compatibility
- No regression found in US1 gateway test reruns after boundary/context safety fixes.
- Existing CLI/adapter compatibility preserved by passing US1 contract and integration suite.

### Art. XI - Traceability and observability safety
- US1 rerun confirms boundary path remains stable after context propagation cleanup and fail-closed handling improvements.
- No runtime test evidence of cross-tenant leakage or uncaught boundary context failures in validated suite.

## Test Execution Notes
Validated from latest Surefire reports in `backend/clinic-gateway-app/target/surefire-reports`:
- `DevProfileTemplateContractTest`: tests=1, failures=0, errors=0, skipped=0
- `TenantBoundaryContractTest`: tests=6, failures=0, errors=0, skipped=0
- `TenantIsolationRLSTest`: tests=6, failures=0, errors=0, skipped=0
- `TenantMissingContextTest`: tests=5, failures=0, errors=0, skipped=0

Additional build evidence:
- `backend/us1_gateway_final.log` contains `BUILD SUCCESS` for focused US1 gateway verification run.

## Decision
US1 constitutional sign-off gaps are closed for progression purposes.
- CHK009: closed
- CHK021: closed
- CHK027: closed
- CHK062: closed

Progression to US2 is now unblocked from the US1 constitutional-evidence perspective.

## US2 Addendum

### Objective
Provide auditable evidence that US2 satisfies the planned constitutional gates for module independence, CLI contracts and release readiness, with focus on Art. II, Art. III, Art. XIII, Art. XIX and Art. XVI.

### Evidence Summary

#### Art. II - CLI contracts per module
- Contract tests exist for the module CLI surfaces planned in US2: `TenantCLIContractTest`, `IAMCLIContractTest` and `ObservabilityCLIContractTest`.
- CLI contracts remain documented in `specs/002-definir-fundacao-modular/contracts/cli-contracts.md` with JSON output and observability fields.
- `CliContextFilter` and `CLIMetrics` complete the runtime evidence that CLI execution is observable and measured across modules.

Evidence files:
- `backend/clinic-tenant-core/src/test/java/com/clinicadigital/tenant/contract/TenantCLIContractTest.java`
- `backend/clinic-iam-core/src/test/java/com/clinicadigital/iam/contract/IAMCLIContractTest.java`
- `backend/clinic-observability-core/src/test/java/com/clinicadigital/observability/contract/ObservabilityCLIContractTest.java`
- `specs/002-definir-fundacao-modular/contracts/cli-contracts.md`

#### Art. III - Library boundaries and public APIs
- Public API boundaries are documented per reusable module in `PUBLIC_API.md` for tenant-core, iam-core and observability-core.
- `backend/docs/ARCHITECTURE.md` records the dependency graph and intended one-way relationships.
- `backend/pom.xml` enforces `BanCircularDependencies`, and isolated module verification was already recorded in Phase 4.D.

Evidence files:
- `backend/clinic-tenant-core/docs/PUBLIC_API.md`
- `backend/clinic-iam-core/docs/PUBLIC_API.md`
- `backend/clinic-observability-core/docs/PUBLIC_API.md`
- `backend/docs/ARCHITECTURE.md`
- `backend/pom.xml`

#### Art. XIII - Up-front technical investigation
- `plan.md` and `research.md` preserve the compared alternatives for IAM hashing, session strategy and RLS design, which remained the governing basis for US2 boundaries.

Evidence files:
- `specs/002-definir-fundacao-modular/plan.md`
- `specs/002-definir-fundacao-modular/research.md`

#### Art. XIX / Art. XVI - Constitutional checklist and SBOM release readiness
- US2 checklist review was executed during Phase 4.F against tasks T066-T084 and reconciled in `checklists/sync.md`.
- The release workflow now covers every backend module intended for release selection, including `clinic-platform-bom`, and builds with `-am` so module-local releases resolve reactor dependencies before generating CycloneDX SBOM artifacts.

Evidence files:
- `specs/002-definir-fundacao-modular/checklists/sync.md`
- `.github/workflows/release.yml`

### Decision
US2 constitutional validation is closed according to the planned scope of Phase 4.
- T083: closed
- T084: closed

Progression to US3 is unblocked from the US2 constitutional and SBOM-release-readiness perspective.
