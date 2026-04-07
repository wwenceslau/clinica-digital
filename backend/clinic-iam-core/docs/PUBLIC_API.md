# clinic-iam-core Public API

## Scope
Public contract for IAM core capabilities.

## Module Boundary
- Allowed internal dependency: `clinic-shared-kernel`
- Must not depend on: `clinic-tenant-core`, `clinic-observability-core`, `clinic-gateway-app`
- Consumed by: `clinic-gateway-app`

## Current Public Surface
At this stage, no production classes are exposed from `src/main/java` in this module.

## Reserved Contract (planned in US3)
- Domain entities: user, role, session, audit event
- Application services: authentication, session management, authorization
- CLI commands: `auth login`, `auth logout`, `auth whoami` with `--json`

## Compatibility and Versioning
- This module follows semantic versioning.
- Until first stable API is published, the public surface is considered pre-stable.
- Once public classes are introduced, MAJOR/MINOR/PATCH rules follow the same policy as the other core modules.
