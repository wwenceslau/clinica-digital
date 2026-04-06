-- iam_audit_events is append-only; INSERT policy + REVOKE of UPDATE/DELETE from app_user.
ALTER TABLE iam_audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_audit_events_tenant_isolation ON iam_audit_events;
CREATE POLICY iam_audit_events_tenant_isolation ON iam_audit_events
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

-- Enforce append-only constraint: revoke mutating privileges from the application role.
-- If app_user role does not exist yet, this is a no-op (idempotent via DO block).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
        REVOKE UPDATE, DELETE ON iam_audit_events FROM app_user;
    END IF;
END$$;
