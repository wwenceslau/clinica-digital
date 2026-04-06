ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenants FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenants_tenant_isolation ON tenants;
CREATE POLICY tenants_tenant_isolation ON tenants
FOR ALL
USING (
    id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    id = current_setting('app.tenant_id', true)::uuid
);
