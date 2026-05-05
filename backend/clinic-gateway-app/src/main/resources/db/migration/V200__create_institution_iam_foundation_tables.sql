CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    cnes VARCHAR(7) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    fhir_resource_id VARCHAR(64) NOT NULL,
    fhir_meta_profile JSONB NOT NULL,
    fhir_identifier_json JSONB NOT NULL,
    fhir_name VARCHAR(255) NOT NULL,
    fhir_active BOOLEAN NOT NULL DEFAULT TRUE,
    quota_tier VARCHAR(32) NOT NULL DEFAULT 'standard',
    account_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_organizations_quota_tier CHECK (quota_tier IN ('standard', 'premium', 'enterprise')),
    CONSTRAINT ck_organizations_meta_profile_not_empty CHECK (jsonb_array_length(fhir_meta_profile) > 0),
    CONSTRAINT ck_organizations_identifier_not_empty CHECK (jsonb_array_length(fhir_identifier_json) > 0),
    CONSTRAINT uq_organizations_cnes UNIQUE (cnes),
    CONSTRAINT uq_organizations_display_name UNIQUE (tenant_id, display_name)
);

CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    fhir_resource_id VARCHAR(64) NOT NULL,
    fhir_meta_profile JSONB NOT NULL,
    fhir_identifier_json JSONB NOT NULL,
    fhir_name VARCHAR(255) NOT NULL,
    fhir_status VARCHAR(32) NOT NULL,
    fhir_mode VARCHAR(32) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    account_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_locations_status CHECK (fhir_status IN ('active', 'suspended', 'inactive')),
    CONSTRAINT ck_locations_mode CHECK (fhir_mode IN ('instance', 'kind')),
    CONSTRAINT ck_locations_meta_profile_not_empty CHECK (jsonb_array_length(fhir_meta_profile) > 0),
    CONSTRAINT ck_locations_identifier_not_empty CHECK (jsonb_array_length(fhir_identifier_json) > 0)
);

CREATE TABLE IF NOT EXISTS practitioners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants (id) ON DELETE CASCADE,
    fhir_resource_id VARCHAR(64) NOT NULL,
    fhir_meta_profile JSONB NOT NULL,
    fhir_identifier_json JSONB NOT NULL,
    fhir_name_json JSONB NOT NULL,
    fhir_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_name VARCHAR(255) NOT NULL,
    cpf_encrypted BYTEA,
    encryption_key_version VARCHAR(32),
    account_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_practitioners_meta_profile_not_empty CHECK (jsonb_array_length(fhir_meta_profile) > 0),
    CONSTRAINT ck_practitioners_identifier_not_empty CHECK (jsonb_array_length(fhir_identifier_json) > 0),
    CONSTRAINT ck_practitioners_name_not_empty CHECK (jsonb_array_length(fhir_name_json) > 0)
);

CREATE TABLE IF NOT EXISTS practitioner_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES locations (id) ON DELETE CASCADE,
    practitioner_id UUID NOT NULL REFERENCES practitioners (id) ON DELETE CASCADE,
    fhir_resource_id VARCHAR(64) NOT NULL,
    fhir_meta_profile JSONB NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    primary_role BOOLEAN NOT NULL DEFAULT FALSE,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_practitioner_roles_meta_profile_not_empty CHECK (jsonb_array_length(fhir_meta_profile) > 0),
    CONSTRAINT uq_practitioner_roles_scope UNIQUE (tenant_id, practitioner_id, location_id, role_code)
);

CREATE TABLE IF NOT EXISTS iam_auth_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    iam_user_id UUID NOT NULL REFERENCES iam_users (id) ON DELETE CASCADE,
    challenge_token_digest TEXT NOT NULL,
    organization_options_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_iam_auth_challenges_org_options_not_empty CHECK (jsonb_array_length(organization_options_json) > 0)
);

CREATE TABLE IF NOT EXISTS iam_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iam_groups_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS iam_user_groups (
    iam_user_id UUID NOT NULL REFERENCES iam_users (id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES iam_groups (id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by_user_id UUID REFERENCES iam_users (id),
    PRIMARY KEY (iam_user_id, group_id)
);

ALTER TABLE iam_users
    ADD COLUMN IF NOT EXISTS practitioner_id UUID REFERENCES practitioners (id),
    ADD COLUMN IF NOT EXISTS profile INTEGER NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS failed_login_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

ALTER TABLE iam_sessions
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations (id),
    ADD COLUMN IF NOT EXISTS active_practitioner_role_id UUID REFERENCES practitioner_roles (id),
    ADD COLUMN IF NOT EXISTS opaque_token_digest TEXT,
    ADD COLUMN IF NOT EXISTS client_ip TEXT,
    ADD COLUMN IF NOT EXISTS revocation_reason VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_locations_tenant_org ON locations (tenant_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_practitioner_roles_tenant_practitioner ON practitioner_roles (tenant_id, practitioner_id);
CREATE INDEX IF NOT EXISTS idx_iam_auth_challenges_expires_at ON iam_auth_challenges (expires_at);
CREATE INDEX IF NOT EXISTS idx_iam_user_groups_group_id ON iam_user_groups (group_id);
CREATE INDEX IF NOT EXISTS idx_iam_sessions_tenant_expires ON iam_sessions (tenant_id, expires_at);
