ALTER TABLE iam_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_users FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_users_tenant_isolation ON iam_users;
CREATE POLICY iam_users_tenant_isolation ON iam_users
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);
