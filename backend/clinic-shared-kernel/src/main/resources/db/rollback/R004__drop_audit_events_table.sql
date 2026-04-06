DROP TRIGGER IF EXISTS trg_iam_audit_events_no_mutation ON iam_audit_events;
DROP FUNCTION IF EXISTS iam_audit_events_append_only_guard();
DROP TABLE IF EXISTS iam_audit_events;
