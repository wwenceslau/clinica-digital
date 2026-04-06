-- Restore UPDATE/DELETE for app_user if role exists (inverse of V011 REVOKE).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
        GRANT UPDATE, DELETE ON iam_audit_events TO app_user;
    END IF;
END$$;

DROP POLICY IF EXISTS iam_audit_events_tenant_isolation ON iam_audit_events;
ALTER TABLE iam_audit_events NO FORCE ROW LEVEL SECURITY;
ALTER TABLE iam_audit_events DISABLE ROW LEVEL SECURITY;
