# clinic-observability-core Public API

## Scope
Public contract for structured logging and tenant-aware metrics.

## Module Boundary
- Allowed internal dependency: `clinic-shared-kernel`
- Must not depend on: `clinic-tenant-core`, `clinic-iam-core`, `clinic-gateway-app`
- Consumed by: `clinic-gateway-app`

## Public Types
- `com.clinicadigital.observability.JsonLogger`
- `com.clinicadigital.observability.TenantAwareMeterRegistry`

## Public Methods
### JsonLogger
- `JsonLogger(String tenantId)`
- `JsonLogger(String tenantId, String traceId)`
- `void logOperationStart(String operation, String message)`
- `void logSuccess(String operation, String message)`
- `void logFailure(String operation, String message, Throwable exception)`
- `void logPartial(String operation, String message)`
- `void logSecurityEvent(String operation, String message)`
- `String getTraceId()`
- `String getTenantId()`
- `static void clearContext()`
- `static Map<String, String> getCurrentContext()`

### TenantAwareMeterRegistry
- `TenantAwareMeterRegistry(MeterRegistry delegate)`
- `Timer timer(String name, String... tags)`
- `Counter counter(String name, String... tags)`
- `<T> T recordCallableWithTenant(String name, Callable<T> callable, String... tags)`
- `void recordRunnableWithTenant(String name, Runnable runnable, String... tags)`
- `void recordFailure(String operationName, String exceptionType, String... tags)`
- `void recordSuccess(String operationName, String... tags)`
- `void recordQuotaExceeded(String quotaName)`
- `MeterRegistry getDelegate()`

## Compatibility and Versioning
- This module follows semantic versioning.
- MAJOR: breaking changes in method signatures, constructor contracts, or expected MDC/metric behavior.
- MINOR: new backward-compatible methods or tags.
- PATCH: bug fixes that preserve the public contract.
- Backward compatibility must be validated by module contract tests before release.
