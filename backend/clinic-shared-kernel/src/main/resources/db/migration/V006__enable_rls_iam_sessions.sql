ALTER TABLE iam_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_sessions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_sessions_tenant_isolation ON iam_sessions;
CREATE POLICY iam_sessions_tenant_isolation ON iam_sessions
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);
