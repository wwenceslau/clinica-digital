-- iam_user_roles has no tenant_id column; isolation is enforced via JOIN on iam_roles.
ALTER TABLE iam_user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user_roles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_user_roles_tenant_isolation ON iam_user_roles;
CREATE POLICY iam_user_roles_tenant_isolation ON iam_user_roles
FOR ALL
USING (
    role_id IN (
        SELECT id FROM iam_roles
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    )
)
WITH CHECK (
    role_id IN (
        SELECT id FROM iam_roles
        WHERE tenant_id = current_setting('app.tenant_id', true)::uuid
    )
);
