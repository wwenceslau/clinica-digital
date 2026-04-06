CREATE INDEX IF NOT EXISTS idx_iam_users_tenant_username
    ON iam_users (tenant_id, username);

CREATE INDEX IF NOT EXISTS idx_iam_users_tenant_email
    ON iam_users (tenant_id, email);

CREATE INDEX IF NOT EXISTS idx_iam_sessions_tenant_expires_at
    ON iam_sessions (tenant_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_iam_audit_events_tenant_created_at
    ON iam_audit_events (tenant_id, created_at);
