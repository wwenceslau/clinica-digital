package com.clinicadigital.iam.test;

import com.clinicadigital.iam.application.PasswordService;
import com.clinicadigital.iam.application.SessionManager;
import com.clinicadigital.iam.domain.IamSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for IAM integration tests.
 *
 * Provides:
 * - TestContainers PostgreSQL container with RLS extensions
 * - MockMvc for HTTP endpoint testing
 * - Fixtures for tenant, organization, user test data
 * - Contract test helpers for CLI and API
 *
 * Architecture: Feature 004 - Institution IAM Authentication Integration
 * Tests are MANDATORY. Every user story starts with failing tests before implementation.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIAMTest {

    /** Stable UUID for the system-level tenant used by super-user (profile 0) fixtures. */
    protected static final UUID SYSTEM_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("clinica_test")
        .withUsername("test_user")
        .withPassword("test_password");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Ensure system tenant exists for super-user fixtures
        jdbcTemplate.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, 'system', 'System Tenant', 'active', 'basic',
                    999999, 999, 999999, NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """, SYSTEM_TENANT_ID);
    }

    /**
     * Fixtures: Create super-user (profile 0) for bootstrap tests.
     */
    protected void createTestSuperUser(String email, String password) {
        String passwordHash = passwordService.hashPassword(password);
        jdbcTemplate.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, is_active, profile, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, 0, NOW(), NOW())
                """,
                UUID.randomUUID(), SYSTEM_TENANT_ID, email, email, passwordHash);
    }

    /**
     * Fixtures: Create tenant organization and admin (profile 10).
     */
    protected UUID createTestTenantWithAdmin(String tenantName, String cnes, String adminEmail) {
        UUID tenantId = UUID.randomUUID();
        String slug = tenantName.toLowerCase().replaceAll("[^a-z0-9]", "-");

        jdbcTemplate.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'basic', 120, 10, 1024, NOW(), NOW())
                """,
                tenantId, slug, tenantName);

        String fhirProfile = "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
        String fhirIdentifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"" + cnes + "\"}]";
        jdbcTemplate.update("""
                INSERT INTO organizations (id, tenant_id, cnes, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, true, NOW(), NOW())
                """,
                UUID.randomUUID(), tenantId, cnes, tenantName,
                "org-" + tenantId, fhirProfile, fhirIdentifier, tenantName);

        String passwordHash = passwordService.hashPassword(adminEmail + "-default");
        jdbcTemplate.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, is_active, profile, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, 10, NOW(), NOW())
                """,
                UUID.randomUUID(), tenantId, adminEmail, adminEmail, passwordHash);

        return tenantId;
    }

    /**
     * Fixtures: Create user (profile 20) within tenant.
     */
    protected UUID createTestUser(UUID tenantId, String email, String password, int profile) {
        UUID userId = UUID.randomUUID();
        String passwordHash = passwordService.hashPassword(password);
        jdbcTemplate.update("""
                INSERT INTO iam_users (id, tenant_id, username, email,
                    password_hash, password_algo, is_active, profile, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'bcrypt', true, ?, NOW(), NOW())
                """,
                userId, tenantId, email, email, passwordHash, profile);
        return userId;
    }

    /**
     * Fixtures: Create and return valid opaque session token.
     * Creates a temporary user in the given tenant to own the session.
     */
    protected String createTestSessionToken(UUID tenantId, UUID organizationId, UUID practitionerRoleId) {
        UUID userId = createTestUser(
                tenantId,
                "session-fixture-" + UUID.randomUUID() + "@test.local",
                "fixture-password",
                20);
        IamSession session = sessionManager.createSession(
                userId, tenantId, "test-trace-" + practitionerRoleId);
        return session.id().toString();
    }

    /**
     * Contract Test Helper: Parse FHIR OperationOutcome from response.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseOperationOutcome(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            throw new AssertionError("Cannot parse OperationOutcome JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Contract Test Helper: Assert that response is valid OperationOutcome error.
     */
    @SuppressWarnings("unchecked")
    protected void assertOperationOutcomeError(String jsonResponse, String expectedCode, String expectedSeverity) {
        Map<String, Object> outcome = parseOperationOutcome(jsonResponse);
        List<Map<String, Object>> issues = (List<Map<String, Object>>) outcome.get("issue");
        Assertions.assertThat(issues)
                .as("OperationOutcome must contain non-empty 'issue' array (Art. VI)")
                .isNotNull()
                .isNotEmpty();
        Map<String, Object> first = issues.get(0);
        Assertions.assertThat(first.get("severity"))
                .as("OperationOutcome issue[0].severity")
                .isEqualTo(expectedSeverity);
        Assertions.assertThat(first.get("code"))
                .as("OperationOutcome issue[0].code")
                .isEqualTo(expectedCode);
    }
}

