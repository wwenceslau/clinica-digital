CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT NOT NULL UNIQUE,
    legal_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',
    plan_tier TEXT NOT NULL,
    quota_requests_per_minute INTEGER NOT NULL DEFAULT 60,
    quota_concurrency INTEGER NOT NULL DEFAULT 10,
    quota_storage_mb INTEGER NOT NULL DEFAULT 1024,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants (status);
