package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.BootstrapSuperUserService;
import com.clinicadigital.iam.application.SuperUserAlreadyExistsException;
import com.clinicadigital.iam.test.BaseIAMTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T023 [P] [US1] Integration test: super-user uniqueness enforcement.
 *
 * Verifies that the bootstrap operation is idempotent-safe: a second bootstrap
 * attempt is rejected with {@link SuperUserAlreadyExistsException}, ensuring
 * only one super-user (profile 0) can exist in the system.
 *
 * Independent test: runs against a real PostgreSQL container (Testcontainers).
 * Refs: FR-001, FR-002
 */
class SuperUserUniquenessIntegrationTest extends BaseIAMTest {

    @Autowired
    private BootstrapSuperUserService bootstrapSuperUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanSuperUser() {
        // Remove any super-user left from previous test runs (profile=0 + global practitioner)
        jdbcTemplate.update(
                "DELETE FROM iam_users WHERE profile = 0 AND tenant_id = ?",
                SYSTEM_TENANT_ID);
        jdbcTemplate.update(
                "DELETE FROM practitioners WHERE tenant_id IS NULL");
    }

    @Test
    void bootstrapOnEmptyDatabaseSucceeds() {
        var result = assertDoesNotThrow(() ->
                bootstrapSuperUserService.bootstrap("admin@system.local", "S3cr3t!Pass", "Admin System"));

        assertNotNull(result.practitionerId(), "practitionerId must not be null");
        assertNotNull(result.auditEventId(), "auditEventId must not be null");
    }

    @Test
    void bootstrapSecondTimeThrowsSuperUserAlreadyExistsException() {
        // First bootstrap succeeds
        bootstrapSuperUserService.bootstrap("admin@system.local", "S3cr3t!Pass", "Admin System");

        // Second bootstrap must be rejected
        assertThrows(
                SuperUserAlreadyExistsException.class,
                () -> bootstrapSuperUserService.bootstrap("another@system.local", "S3cr3t!Pass", "Another"),
                "second bootstrap must throw SuperUserAlreadyExistsException"
        );
    }

    @Test
    void onlyOneSuperUserExistsAfterSuccessfulBootstrap() {
        bootstrapSuperUserService.bootstrap("admin@system.local", "S3cr3t!Pass", "Admin System");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_users WHERE profile = 0",
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count,
                "exactly one super-user (profile=0) must exist after bootstrap");
    }
}
