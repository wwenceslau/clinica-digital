DROP POLICY IF EXISTS iam_sessions_tenant_isolation ON iam_sessions;
ALTER TABLE iam_sessions NO FORCE ROW LEVEL SECURITY;
ALTER TABLE iam_sessions DISABLE ROW LEVEL SECURITY;
