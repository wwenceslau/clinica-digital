-- =============================================================================
-- V205__fix_practitioner_pii_nullable_for_superuser.sql
--
-- V203 section 3b set cpf_encrypted and encryption_key_version NOT NULL, but
-- the global super-user (profile 0, tenant_id IS NULL) is a system entity with
-- no real CPF — BootstrapSuperUserService creates a Practitioner without PII.
--
-- Fix: drop the blanket NOT NULL and replace with a conditional CHECK that
-- requires PII only for tenant-scoped practitioners (tenant_id IS NOT NULL).
-- =============================================================================

-- 1. Drop the blanket NOT NULL on both PII columns
ALTER TABLE practitioners
    ALTER COLUMN cpf_encrypted          DROP NOT NULL,
    ALTER COLUMN encryption_key_version DROP NOT NULL;

-- 2. Conditional PII requirement: only mandatory for tenant-scoped practitioners
ALTER TABLE practitioners
    ADD CONSTRAINT ck_practitioners_pii_required_for_tenant
        CHECK (
            tenant_id IS NULL
            OR (cpf_encrypted IS NOT NULL AND encryption_key_version IS NOT NULL)
        );

-- 3. Re-scope the existing non-empty check to be NULL-safe (NULL is now allowed)
ALTER TABLE practitioners
    DROP CONSTRAINT IF EXISTS ck_practitioners_encryption_key_version_not_empty;

ALTER TABLE practitioners
    ADD CONSTRAINT ck_practitioners_encryption_key_version_not_empty
        CHECK (encryption_key_version IS NULL OR length(encryption_key_version) > 0);
