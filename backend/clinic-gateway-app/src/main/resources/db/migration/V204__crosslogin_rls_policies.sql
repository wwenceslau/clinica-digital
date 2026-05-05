-- =============================================================================
-- V204__crosslogin_rls_policies.sql
--
-- Adds permissive RLS policies for the US4 multi-org cross-tenant login flow.
--
-- During the login flow the caller has no tenant context (no X-Tenant-ID header),
-- so the standard RLS policy (tenant_id = app.tenant_id) blocks all rows.
-- To allow trusted cross-tenant operations within a controlled transaction the
-- application sets the PostgreSQL session variable app.crosslogin = 'true' (via
-- SET LOCAL, which is auto-reset on transaction commit/rollback).
--
-- Affected tables:
--   iam_users           — SELECT by email for credential verification
--   iam_auth_challenges — full DML for challenge create/resolve
--   iam_sessions        — INSERT for session creation after org selection
--   iam_audit_events    — INSERT for global (null-tenant) audit events
--
-- Security: SET LOCAL is limited to the current transaction; no persistent
-- bypass is possible. All crosslogin operations must be @Transactional.
--
-- Refs: FR-004, FR-007, US4; specs/004-institution-iam-auth-integration/plan.md
-- =============================================================================

-- ---------------------------------------------------------------------------
-- iam_users: cross-tenant SELECT for email-based login lookup
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS iam_users_crosslogin_select ON iam_users;
CREATE POLICY iam_users_crosslogin_select ON iam_users
FOR SELECT
USING (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- iam_auth_challenges: full DML (CREATE challenge, RESOLVE challenge)
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS iam_auth_challenges_crosslogin ON iam_auth_challenges;
CREATE POLICY iam_auth_challenges_crosslogin ON iam_auth_challenges
FOR ALL
USING  (current_setting('app.crosslogin', true) = 'true')
WITH CHECK (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- iam_sessions: INSERT when creating a session for an authenticated user
-- (the tenant_id will be the chosen org UUID, set by the application)
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS iam_sessions_crosslogin_insert ON iam_sessions;
CREATE POLICY iam_sessions_crosslogin_insert ON iam_sessions
FOR INSERT
WITH CHECK (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- iam_audit_events: INSERT for global (null-tenant) events during login
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS iam_audit_events_crosslogin_insert ON iam_audit_events;
CREATE POLICY iam_audit_events_crosslogin_insert ON iam_audit_events
FOR INSERT
WITH CHECK (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- organizations: SELECT for cross-tenant org resolution during login
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS organizations_crosslogin_select ON organizations;
CREATE POLICY organizations_crosslogin_select ON organizations
FOR SELECT
USING (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- practitioner_roles: SELECT for cross-tenant role lookup during login
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS practitioner_roles_crosslogin_select ON practitioner_roles;
CREATE POLICY practitioner_roles_crosslogin_select ON practitioner_roles
FOR SELECT
USING (current_setting('app.crosslogin', true) = 'true');

-- ---------------------------------------------------------------------------
-- iam_users: add practitioner_id FK column (missing from V203, needed by
-- IamUser entity for loginByEmail practitioner role resolution)
-- ---------------------------------------------------------------------------
ALTER TABLE iam_users
    ADD COLUMN IF NOT EXISTS practitioner_id UUID REFERENCES practitioners (id);
