# clinic-tenant-core Public API

## Scope
Public contract for tenant domain operations and tenant CLI integration.

## Module Boundary
- Allowed internal dependency: `clinic-shared-kernel`
- Must not depend on: `clinic-iam-core`, `clinic-observability-core`, `clinic-gateway-app`
- Consumed by: `clinic-gateway-app`

## Public Types
- `com.clinicadigital.tenant.domain.Tenant`
- `com.clinicadigital.tenant.domain.ITenantRepository`
- `com.clinicadigital.tenant.application.TenantService`
- `com.clinicadigital.tenant.application.QuotaService`
- `com.clinicadigital.tenant.application.QuotaExceededException`
- `com.clinicadigital.tenant.cli.TenantCommands`
- `com.clinicadigital.tenant.infrastructure.TenantRepository`

## Public Methods
### TenantService
- `Tenant createTenant(String slug, String legalName, String planTier)`
- `Tenant getTenant(UUID tenantId)`
- `List<Tenant> listTenants()`
- `Tenant updateQuota(UUID tenantId, Integer requestsPerMinute, Integer concurrency, Integer storageMb)`

### QuotaService
- `void checkAndEnforceQuota(UUID tenantId, String metric)`
- `String HTTP_REQUEST_METRIC` (public constant)

### ITenantRepository
- `Optional<Tenant> findBySlug(String slug)`
- `Optional<Tenant> findById(UUID id)`
- `List<Tenant> findAll()`
- `Tenant save(Tenant tenant)`

### TenantCommands (CLI)
- `tenant create --slug --legal-name --plan-tier [--json]`
- `tenant list [--json]`

## DTO/Domain Payload Notes
### Tenant
- Identity: `id`, `slug`
- Business fields: `legalName`, `status`, `planTier`
- Quota fields: `quotaRequestsPerMinute`, `quotaConcurrency`, `quotaStorageMb`
- Audit timestamps: `createdAt`, `updatedAt`

## Compatibility and Versioning
- This module follows semantic versioning.
- MAJOR: breaking signature/behavior changes in any public type above.
- MINOR: backward-compatible additions.
- PATCH: internal fixes without API contract changes.
- Deprecated APIs must remain available for at least one MINOR release before removal.
