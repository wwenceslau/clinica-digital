CREATE TABLE IF NOT EXISTS iam_audit_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    actor_user_id UUID,
    event_type TEXT NOT NULL,
    outcome TEXT NOT NULL,
    trace_id TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enforce append-only semantics via trigger.
CREATE OR REPLACE FUNCTION iam_audit_events_append_only_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'iam_audit_events is append-only';
END;
$$;

DROP TRIGGER IF EXISTS trg_iam_audit_events_no_mutation ON iam_audit_events;
CREATE TRIGGER trg_iam_audit_events_no_mutation
BEFORE UPDATE OR DELETE ON iam_audit_events
FOR EACH ROW
EXECUTE FUNCTION iam_audit_events_append_only_guard();
