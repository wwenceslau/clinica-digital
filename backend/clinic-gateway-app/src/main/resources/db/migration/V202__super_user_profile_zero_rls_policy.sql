DO $$
BEGIN
    IF current_setting('app.profile', true) IS NULL THEN
        PERFORM set_config('app.profile', '', true);
    END IF;
END$$;

DROP POLICY IF EXISTS organizations_super_user_profile_0 ON organizations;
CREATE POLICY organizations_super_user_profile_0 ON organizations
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS locations_super_user_profile_0 ON locations;
CREATE POLICY locations_super_user_profile_0 ON locations
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS practitioners_super_user_profile_0 ON practitioners;
CREATE POLICY practitioners_super_user_profile_0 ON practitioners
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS practitioner_roles_super_user_profile_0 ON practitioner_roles;
CREATE POLICY practitioner_roles_super_user_profile_0 ON practitioner_roles
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS iam_users_super_user_profile_0 ON iam_users;
CREATE POLICY iam_users_super_user_profile_0 ON iam_users
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS iam_groups_super_user_profile_0 ON iam_groups;
CREATE POLICY iam_groups_super_user_profile_0 ON iam_groups
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS iam_user_groups_super_user_profile_0 ON iam_user_groups;
CREATE POLICY iam_user_groups_super_user_profile_0 ON iam_user_groups
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS iam_sessions_super_user_profile_0 ON iam_sessions;
CREATE POLICY iam_sessions_super_user_profile_0 ON iam_sessions
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');

DROP POLICY IF EXISTS iam_audit_events_super_user_profile_0 ON iam_audit_events;
CREATE POLICY iam_audit_events_super_user_profile_0 ON iam_audit_events
FOR ALL
USING (current_setting('app.profile', true) = '0')
WITH CHECK (current_setting('app.profile', true) = '0');
