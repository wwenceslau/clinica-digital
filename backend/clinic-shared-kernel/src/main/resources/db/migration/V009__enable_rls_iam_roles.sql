ALTER TABLE iam_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_roles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_roles_tenant_isolation ON iam_roles;
CREATE POLICY iam_roles_tenant_isolation ON iam_roles
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);
