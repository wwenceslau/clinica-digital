package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.CreateProfile20UserService;
import com.clinicadigital.iam.application.CreateProfile20UserService.EmailAlreadyTakenException;
import com.clinicadigital.iam.application.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T091 [P] Integration test: email conflict on profile 20 user creation.
 *
 * <p>TDD state: RED until {@link CreateProfile20UserService} is implemented (T094).
 *
 * <p>Scenarios:
 * <ul>
 *   <li>Same email within same tenant → throws {@link EmailAlreadyTakenException} (FR-009).</li>
 *   <li>Same email in different tenant → succeeds (emails unique per-tenant for profiles 10/20).</li>
 * </ul>
 *
 * Refs: FR-006, FR-009
 */
@SpringBootTest
@Testcontainers
class CreateProfile20EmailConflictTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC091\"}]";
    private static final String FHIR_IDENTIFIER_JSON =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"T091001\"}]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T091\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_user20_email_conflict_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    CreateProfile20UserService createProfile20UserService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordService passwordService;

    @BeforeEach
    void cleanTestData() {
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");
        jdbc.execute("TRUNCATE TABLE tenants CASCADE");
    }

    /**
     * Same email within same tenant → {@link EmailAlreadyTakenException}.
     */
    @Test
    void sameEmailInSameTenantThrowsEmailAlreadyTakenException() {
        UUID tenantId = insertTenant("t091-dup", "Tenant T091 Dup");
        UUID orgId = insertOrganization(tenantId, "T091001", "Org T091 Dup");
        UUID locationId = insertLocation(tenantId, orgId, "Location T091 Dup");

        // Create first user successfully
        createProfile20UserService.create(
                tenantId, orgId, locationId,
                "First User T091",
                "shared@t091.local",
                "11111111111",
                "S3nha@First",
                "RN",
                UUID.randomUUID());

        // Attempt to create second user with same email in same tenant → must throw
        assertThatThrownBy(() ->
                createProfile20UserService.create(
                        tenantId, orgId, locationId,
                        "Second User T091",
                        "shared@t091.local",
                        "22222222222",
                        "S3nha@Second",
                        "MD",
                        UUID.randomUUID()))
                .isInstanceOf(EmailAlreadyTakenException.class)
                .hasMessageContaining("shared@t091.local");
    }

    /**
     * Same email in different tenant → allowed (creates successfully).
     */
    @Test
    void sameEmailInDifferentTenantSucceeds() {
        UUID tenantAId = insertTenant("t091-ta", "Tenant A T091");
        UUID orgAId = insertOrganization(tenantAId, "T091002", "Org A T091");
        UUID locationAId = insertLocation(tenantAId, orgAId, "Location A T091");

        UUID tenantBId = insertTenant("t091-tb", "Tenant B T091");
        UUID orgBId = insertOrganization(tenantBId, "T091003", "Org B T091");
        UUID locationBId = insertLocation(tenantBId, orgBId, "Location B T091");

        // Create in tenantA
        createProfile20UserService.create(
                tenantAId, orgAId, locationAId,
                "User In A T091",
                "crossshard@t091.local",
                "33333333333",
                "S3nha@InA",
                "RN",
                UUID.randomUUID());

        // Same email in tenantB → should succeed
        var resultB = createProfile20UserService.create(
                tenantBId, orgBId, locationBId,
                "User In B T091",
                "crossshard@t091.local",
                "44444444444",
                "S3nha@InB",
                "MD",
                UUID.randomUUID());

        assertThat(resultB.userId())
                .as("T091: same email in different tenant must succeed and return userId")
                .isNotNull();
    }

    // ---- Helpers ----

    private UUID insertTenant(String slug, String legalName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tenants (id, slug, legal_name, status, plan_tier,
                    quota_requests_per_minute, quota_concurrency, quota_storage_mb,
                    created_at, updated_at)
                VALUES (?, ?, ?, 'active', 'standard', 120, 10, 1024, NOW(), NOW())
                """, id, slug, legalName);
        return id;
    }

    private UUID insertOrganization(UUID tenantId, String cnes, String displayName) {
        UUID id = tenantId; // ck_organizations_tenant_is_self
        String identifier = "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"" + cnes + "\"}]";
        jdbc.update("""
                INSERT INTO organizations (id, tenant_id, cnes, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_active, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, true, true, NOW(), NOW())
                """, id, tenantId, cnes, displayName,
                "org-" + id, FHIR_ORG_PROFILE, identifier, displayName);
        return id;
    }

    private UUID insertLocation(UUID tenantId, UUID orgId, String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO locations (id, tenant_id, organization_id, display_name,
                    fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                    fhir_name, fhir_status, fhir_mode, account_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, 'active', 'instance', true, NOW(), NOW())
                """, id, tenantId, orgId, displayName,
                "loc-" + id, LOC_PROFILE, LOC_IDENTIFIER, displayName);
        return id;
    }
}
