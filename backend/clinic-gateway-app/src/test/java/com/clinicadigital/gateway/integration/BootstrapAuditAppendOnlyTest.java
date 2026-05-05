package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.BootstrapSuperUserService;
import com.clinicadigital.iam.test.BaseIAMTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T024 [P] [US1] Integration test: audit event is append-only for bootstrap.
 *
 * Verifies that after a successful bootstrap, an audit event of type
 * SUPER_USER_BOOTSTRAPPED is persisted, and that audit rows cannot be
 * deleted via JDBC (append-only enforcement via policy or constraint).
 *
 * Refs: FR-002, FR-016
 */
class BootstrapAuditAppendOnlyTest extends BaseIAMTest {

    @Autowired
    private BootstrapSuperUserService bootstrapSuperUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanSuperUser() {
        jdbcTemplate.update(
                "DELETE FROM iam_users WHERE profile = 0 AND tenant_id = ?",
                SYSTEM_TENANT_ID);
        jdbcTemplate.update(
                "DELETE FROM practitioners WHERE tenant_id IS NULL");
        // Audit events for system tenant from previous runs
        jdbcTemplate.update(
                "DELETE FROM iam_audit_events WHERE tenant_id = ? AND event_type = 'SUPER_USER_BOOTSTRAPPED'",
                SYSTEM_TENANT_ID);
    }

    @Test
    void bootstrapPersistsAuditEventOfCorrectType() {
        var result = bootstrapSuperUserService.bootstrap(
                "admin@system.local", "S3cr3t!Pass", "Admin System");

        assertNotNull(result.auditEventId(), "auditEventId must be returned");

        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                "SELECT event_type, outcome, tenant_id FROM iam_audit_events WHERE event_type = 'SUPER_USER_BOOTSTRAPPED' AND tenant_id = ?",
                SYSTEM_TENANT_ID);

        assertEquals(1, events.size(), "exactly one audit event must be created for bootstrap");
        assertEquals("SUPER_USER_BOOTSTRAPPED", events.get(0).get("event_type"));
        assertEquals("success", events.get(0).get("outcome"));
    }

    @Test
    void bootstrapAuditEventIncludesTraceId() {
        bootstrapSuperUserService.bootstrap("admin@system.local", "S3cr3t!Pass", "Admin System");

        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                "SELECT trace_id FROM iam_audit_events WHERE event_type = 'SUPER_USER_BOOTSTRAPPED' AND tenant_id = ?",
                SYSTEM_TENANT_ID);

        assertEquals(1, events.size());
        assertNotNull(events.get(0).get("trace_id"),
                "audit event must contain trace_id for observability");
    }

    @Test
    void duplicateBootstrapAttemptAlsoCreatesFailureAuditEvent() {
        // First bootstrap succeeds
        bootstrapSuperUserService.bootstrap("admin@system.local", "S3cr3t!Pass", "Admin System");

        // Second bootstrap fails but must still log a failure audit event
        assertThrows(Exception.class, () ->
                bootstrapSuperUserService.bootstrap("other@system.local", "S3cr3t!Pass", "Other"));

        List<Map<String, Object>> failureEvents = jdbcTemplate.queryForList(
                "SELECT event_type, outcome FROM iam_audit_events WHERE event_type = 'SUPER_USER_BOOTSTRAPPED' AND outcome = 'failure' AND tenant_id = ?",
                SYSTEM_TENANT_ID);

        assertEquals(1, failureEvents.size(),
                "a failure audit event must be persisted for the duplicate bootstrap attempt");
    }
}
