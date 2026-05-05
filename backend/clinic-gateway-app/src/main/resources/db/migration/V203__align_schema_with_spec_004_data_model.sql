-- =============================================================================
-- V203__align_schema_with_spec_004_data_model.sql
--
-- Comprehensive alignment of V200-V202 applied schema with the authoritative
-- data-model defined in:
--   specs/004-institution-iam-auth-integration/data-model.md
--
-- SECTIONS
--   1.  organizations        — FHIR columns, CHECK(tenant_id=id), CI display_name unique
--   2.  locations            — FK tenant→organizations, FHIR columns, CI unique
--   3.  practitioners        — FK tenant→organizations, NOT NULL PII, FHIR columns
--   4.  practitioner_roles   — FK tenant→organizations, FHIR columns, primary_role unique
--   5.  iam_users            — rename is_active→account_active, nullable tenant_id,
--                              FK→organizations, CHECK(profile), CI email unique
--   6.  iam_sessions         — rename user_id→iam_user_id, nullable tenant_id,
--                              FK tenant→organizations, created_at, active flag
--   7.  iam_groups           — FK tenant→organizations, CI name unique
--   8.  iam_permissions      — rename permission_key→code, add resource + action
--   9.  iam_group_permissions — new table + RLS + super-user bypass
--   10. iam_audit_events     — UUID PK, nullable tenant_id, FK→organizations,
--                              actor_user_id FK, actor_practitioner_id, payload_json
--
-- JAVA ENTITIES THAT REQUIRE COLUMN RENAMES AFTER THIS MIGRATION:
--   IamUser.java          is_active       → account_active
--   IamSession.java       user_id         → iam_user_id
--   IamPermission.java    permission_key  → code   (+ new fields resource, action)
--   IamAuditEvent.java    id type         → UUID   (was Long/BIGSERIAL)
--
-- Assumption: the database was migrated to V202 and contains no application data
-- (safe to SET NOT NULL and change PK type on empty tables).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. organizations
-- ---------------------------------------------------------------------------

-- 1a. Optional FHIR extension columns (nullable per spec)
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS fhir_type_json            JSONB,
    ADD COLUMN IF NOT EXISTS fhir_alias_json           JSONB,
    ADD COLUMN IF NOT EXISTS fhir_telecom_json         JSONB,
    ADD COLUMN IF NOT EXISTS fhir_address_json         JSONB,
    ADD COLUMN IF NOT EXISTS fhir_part_of_org_id       UUID REFERENCES organizations (id),
    ADD COLUMN IF NOT EXISTS fhir_endpoint_refs_json   JSONB;

-- 1b. Org IS the tenant root: its own id must equal its tenant_id.
--     The FK to tenants(id) (from V200) is retained; the CHECK ensures they match.
ALTER TABLE organizations
    ADD CONSTRAINT ck_organizations_tenant_is_self CHECK (tenant_id = id);

-- 1c. Global case-insensitive unique on display_name
--     (spec: global, not per-tenant; replaces the V200 per-tenant UNIQUE)
ALTER TABLE organizations DROP CONSTRAINT IF EXISTS uq_organizations_display_name;
CREATE UNIQUE INDEX IF NOT EXISTS uq_organizations_display_name_ci
    ON organizations (lower(display_name));

-- ---------------------------------------------------------------------------
-- 2. locations
-- ---------------------------------------------------------------------------

-- 2a. Re-target tenant_id FK: tenants → organizations
--     (organizations is the authoritative tenant root per spec 004)
ALTER TABLE locations DROP CONSTRAINT IF EXISTS locations_tenant_id_fkey;
ALTER TABLE locations
    ADD CONSTRAINT locations_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 2b. Case-insensitive unique per tenant (spec: UNIQUE(tenant_id, lower(display_name)))
CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_tenant_display_name_ci
    ON locations (tenant_id, lower(display_name));

-- 2c. Optional FHIR extension columns
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS fhir_telecom_json JSONB,
    ADD COLUMN IF NOT EXISTS fhir_address_json JSONB;

-- ---------------------------------------------------------------------------
-- 3. practitioners
-- ---------------------------------------------------------------------------

-- 3a. Re-target tenant_id FK: tenants → organizations
--     (practitioners.tenant_id is already nullable in V200 — null = global super-user)
ALTER TABLE practitioners DROP CONSTRAINT IF EXISTS practitioners_tenant_id_fkey;
ALTER TABLE practitioners
    ADD CONSTRAINT practitioners_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 3b. PII columns are required per spec; table is empty — SET NOT NULL is safe
ALTER TABLE practitioners
    ALTER COLUMN cpf_encrypted          SET NOT NULL,
    ALTER COLUMN encryption_key_version SET NOT NULL;

ALTER TABLE practitioners
    ADD CONSTRAINT ck_practitioners_encryption_key_version_not_empty
        CHECK (length(encryption_key_version) > 0);

-- 3c. Optional FHIR extension columns
ALTER TABLE practitioners
    ADD COLUMN IF NOT EXISTS fhir_telecom_json         JSONB,
    ADD COLUMN IF NOT EXISTS fhir_address_json         JSONB,
    ADD COLUMN IF NOT EXISTS fhir_gender               VARCHAR(16),
    ADD COLUMN IF NOT EXISTS fhir_birth_date           DATE,
    ADD COLUMN IF NOT EXISTS fhir_qualification_json   JSONB,
    ADD COLUMN IF NOT EXISTS fhir_communication_json   JSONB;

ALTER TABLE practitioners
    ADD CONSTRAINT ck_practitioners_fhir_gender
        CHECK (fhir_gender IS NULL OR fhir_gender IN ('male', 'female', 'other', 'unknown'));

-- ---------------------------------------------------------------------------
-- 4. practitioner_roles
-- ---------------------------------------------------------------------------

-- 4a. Re-target tenant_id FK: tenants → organizations
ALTER TABLE practitioner_roles DROP CONSTRAINT IF EXISTS practitioner_roles_tenant_id_fkey;
ALTER TABLE practitioner_roles
    ADD CONSTRAINT practitioner_roles_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 4b. Optional FHIR extension columns
ALTER TABLE practitioner_roles
    ADD COLUMN IF NOT EXISTS fhir_code_json            JSONB,
    ADD COLUMN IF NOT EXISTS fhir_specialty_json       JSONB,
    ADD COLUMN IF NOT EXISTS fhir_telecom_json         JSONB,
    ADD COLUMN IF NOT EXISTS fhir_available_time_json  JSONB;

-- 4c. Partial unique index: at most one primary_role=true per (tenant, practitioner)
CREATE UNIQUE INDEX IF NOT EXISTS uq_practitioner_roles_primary_per_tenant
    ON practitioner_roles (tenant_id, practitioner_id)
    WHERE primary_role = TRUE;

-- ---------------------------------------------------------------------------
-- 5. iam_users
-- ---------------------------------------------------------------------------

-- 5a. Rename: is_active → account_active
--     (naming convention: is_* prefix is prohibited; see specs/004.../naming-convention.md)
ALTER TABLE iam_users RENAME COLUMN is_active TO account_active;

-- 5b. Make tenant_id nullable and re-target FK: tenants → organizations
--     (null tenant_id = profile-0 super-user, who belongs to no single tenant)
ALTER TABLE iam_users DROP CONSTRAINT IF EXISTS iam_users_tenant_id_fkey;
ALTER TABLE iam_users ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE iam_users
    ADD CONSTRAINT iam_users_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 5c. Profile domain constraint
ALTER TABLE iam_users
    ADD CONSTRAINT ck_iam_users_profile
        CHECK (profile IN (0, 10, 20));

-- 5d. Tenant_id required for tenant-scoped profiles; null for super-user
ALTER TABLE iam_users
    ADD CONSTRAINT ck_iam_users_tenant_required_for_tenant_profiles
        CHECK (
            (profile = 0          AND tenant_id IS NULL) OR
            (profile IN (10, 20)  AND tenant_id IS NOT NULL)
        );

-- 5e. Case-insensitive email uniqueness per spec
--     profile 0: globally unique; profiles 10/20: unique per tenant
ALTER TABLE iam_users DROP CONSTRAINT IF EXISTS uq_iam_users_tenant_email;
CREATE UNIQUE INDEX IF NOT EXISTS uq_iam_users_email_global
    ON iam_users (lower(email))
    WHERE profile = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uq_iam_users_tenant_email_ci
    ON iam_users (tenant_id, lower(email))
    WHERE profile IN (10, 20);

-- ---------------------------------------------------------------------------
-- 6. iam_sessions
-- ---------------------------------------------------------------------------

-- 6a. Rename: user_id → iam_user_id (align with spec entity naming)
--     The FK constraint iam_sessions_user_id_fkey remains functional after rename.
ALTER TABLE iam_sessions RENAME COLUMN user_id TO iam_user_id;

-- 6b. Make tenant_id nullable and re-target FK: tenants → organizations
--     (null = super-user session; not tied to any specific tenant)
ALTER TABLE iam_sessions ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE iam_sessions DROP CONSTRAINT IF EXISTS iam_sessions_tenant_id_fkey;
ALTER TABLE iam_sessions
    ADD CONSTRAINT iam_sessions_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 6c. Add lifecycle timestamp and active flag (both required per spec)
ALTER TABLE iam_sessions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS active     BOOLEAN     NOT NULL DEFAULT TRUE;

-- ---------------------------------------------------------------------------
-- 7. iam_groups
-- ---------------------------------------------------------------------------

-- 7a. Re-target tenant_id FK: tenants → organizations
ALTER TABLE iam_groups DROP CONSTRAINT IF EXISTS iam_groups_tenant_id_fkey;
ALTER TABLE iam_groups
    ADD CONSTRAINT iam_groups_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 7b. Case-insensitive name unique per tenant (replaces V200 case-sensitive UNIQUE)
ALTER TABLE iam_groups DROP CONSTRAINT IF EXISTS uq_iam_groups_tenant_name;
CREATE UNIQUE INDEX IF NOT EXISTS uq_iam_groups_tenant_name_ci
    ON iam_groups (tenant_id, lower(name));

-- ---------------------------------------------------------------------------
-- 8. iam_permissions
-- ---------------------------------------------------------------------------

-- 8a. Rename: permission_key → code (align with spec entity naming)
ALTER TABLE iam_permissions RENAME COLUMN permission_key TO code;

-- 8b. Add resource and action dimensions required by spec 004 RBAC model
--     Table is empty — NOT NULL without DEFAULT is safe after adding as NOT NULL DEFAULT
ALTER TABLE iam_permissions
    ADD COLUMN IF NOT EXISTS resource VARCHAR(120) NOT NULL DEFAULT 'unset',
    ADD COLUMN IF NOT EXISTS action   VARCHAR(60)  NOT NULL DEFAULT 'unset';

-- Remove the bootstrap defaults; future INSERTs must supply resource and action
ALTER TABLE iam_permissions
    ALTER COLUMN resource DROP DEFAULT,
    ALTER COLUMN action   DROP DEFAULT;

-- ---------------------------------------------------------------------------
-- 9. iam_group_permissions  (new table — absent from V200-V202)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS iam_group_permissions (
    group_id      UUID NOT NULL REFERENCES iam_groups      (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES iam_permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, permission_id)
);

-- RLS: tenant isolation via iam_groups join
ALTER TABLE iam_group_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_group_permissions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_group_permissions_tenant_isolation ON iam_group_permissions;
CREATE POLICY iam_group_permissions_tenant_isolation ON iam_group_permissions
FOR ALL
USING (
    group_id IN (
        SELECT id FROM iam_groups
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    )
)
WITH CHECK (
    group_id IN (
        SELECT id FROM iam_groups
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    )
);

-- RLS: super-user (profile 0) bypass
DROP POLICY IF EXISTS iam_group_permissions_super_user_profile_0 ON iam_group_permissions;
CREATE POLICY iam_group_permissions_super_user_profile_0 ON iam_group_permissions
FOR ALL
USING  (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

-- ---------------------------------------------------------------------------
-- 10. iam_audit_events
-- ---------------------------------------------------------------------------

-- 10a. Change primary key from BIGSERIAL to UUID (spec requirement)
--      Table is expected to be empty at migration time.
ALTER TABLE iam_audit_events ALTER COLUMN id DROP DEFAULT;
ALTER TABLE iam_audit_events DROP CONSTRAINT iam_audit_events_pkey;
ALTER TABLE iam_audit_events
    ALTER COLUMN id TYPE UUID USING gen_random_uuid();
ALTER TABLE iam_audit_events
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE iam_audit_events
    ADD CONSTRAINT iam_audit_events_pkey PRIMARY KEY (id);
DROP SEQUENCE IF EXISTS iam_audit_events_id_seq;

-- 10b. Allow null tenant_id for global (non-tenant-scoped) audit events
ALTER TABLE iam_audit_events ALTER COLUMN tenant_id DROP NOT NULL;

-- 10c. Re-target tenant_id FK: tenants → organizations
ALTER TABLE iam_audit_events DROP CONSTRAINT IF EXISTS iam_audit_events_tenant_id_fkey;
ALTER TABLE iam_audit_events
    ADD CONSTRAINT iam_audit_events_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- 10d. Add missing FK on actor_user_id (V004 created the column but without a FK)
ALTER TABLE iam_audit_events
    ADD CONSTRAINT iam_audit_events_actor_user_id_fkey
        FOREIGN KEY (actor_user_id) REFERENCES iam_users (id);

-- 10e. Actor practitioner link (complement to actor_user_id for clinical events)
ALTER TABLE iam_audit_events
    ADD COLUMN IF NOT EXISTS actor_practitioner_id UUID REFERENCES practitioners (id);

-- 10f. Spec-aligned payload column
--      metadata_json (V004) is retained for backward compatibility; payload_json is the
--      canonical field going forward.
ALTER TABLE iam_audit_events
    ADD COLUMN IF NOT EXISTS payload_json JSONB;
