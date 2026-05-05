ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS organizations_tenant_isolation ON organizations;
CREATE POLICY organizations_tenant_isolation ON organizations
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

ALTER TABLE locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE locations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS locations_tenant_isolation ON locations;
CREATE POLICY locations_tenant_isolation ON locations
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

ALTER TABLE practitioners ENABLE ROW LEVEL SECURITY;
ALTER TABLE practitioners FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS practitioners_tenant_isolation ON practitioners;
CREATE POLICY practitioners_tenant_isolation ON practitioners
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

ALTER TABLE practitioner_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE practitioner_roles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS practitioner_roles_tenant_isolation ON practitioner_roles;
CREATE POLICY practitioner_roles_tenant_isolation ON practitioner_roles
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

ALTER TABLE iam_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_groups FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_groups_tenant_isolation ON iam_groups;
CREATE POLICY iam_groups_tenant_isolation ON iam_groups
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true)::uuid
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id', true)::uuid
);

ALTER TABLE iam_user_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_user_groups FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_user_groups_tenant_isolation ON iam_user_groups;
CREATE POLICY iam_user_groups_tenant_isolation ON iam_user_groups
FOR ALL
USING (
    EXISTS (
        SELECT 1
        FROM iam_users u
        WHERE u.id = iam_user_id
          AND u.tenant_id = current_setting('app.tenant_id', true)::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM iam_users u
        WHERE u.id = iam_user_id
          AND u.tenant_id = current_setting('app.tenant_id', true)::uuid
    )
);

ALTER TABLE iam_auth_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE iam_auth_challenges FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS iam_auth_challenges_tenant_isolation ON iam_auth_challenges;
CREATE POLICY iam_auth_challenges_tenant_isolation ON iam_auth_challenges
FOR ALL
USING (
    EXISTS (
        SELECT 1
        FROM iam_users u
        WHERE u.id = iam_user_id
          AND u.tenant_id = current_setting('app.tenant_id', true)::uuid
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1
        FROM iam_users u
        WHERE u.id = iam_user_id
          AND u.tenant_id = current_setting('app.tenant_id', true)::uuid
    )
);
