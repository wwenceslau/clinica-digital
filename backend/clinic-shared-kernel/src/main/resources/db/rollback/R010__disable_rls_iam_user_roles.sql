DROP POLICY IF EXISTS iam_user_roles_tenant_isolation ON iam_user_roles;
ALTER TABLE iam_user_roles NO FORCE ROW LEVEL SECURITY;
ALTER TABLE iam_user_roles DISABLE ROW LEVEL SECURITY;
