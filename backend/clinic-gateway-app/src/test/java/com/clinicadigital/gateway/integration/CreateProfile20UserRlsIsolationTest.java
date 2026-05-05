package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.application.CreateProfile20UserService;
import com.clinicadigital.iam.application.PasswordService;
import com.clinicadigital.iam.domain.IamUserRepository;
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

/**
 * T090 [P] Integration test: profile 20 user creation with RLS isolation.
 *
 * <p>TDD state: RED until {@link CreateProfile20UserService} is implemented (T094).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>A profile 20 user created in tenantA has the correct tenant_id set.</li>
 *   <li>Querying iam_users filtered by tenantB returns no results for the user created in tenantA
 *       (RLS tenant isolation — FR-014).</li>
 * </ul>
 *
 * Refs: FR-006, FR-014
 */
@SpringBootTest
@Testcontainers
class CreateProfile20UserRlsIsolationTest {

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLotacaoProfissional\"]";
    private static final String FHIR_PRACTITIONER_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000000\"}]";
    private static final String FHIR_PRACTITIONER_NAME =
            "[{\"use\":\"official\",\"text\":\"Admin T090\"}]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRLocalAtendimento\"]";
    private static final String LOC_IDENTIFIER =
            "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"LOC090\"}]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_user20_rls_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    CreateProfile20UserService createProfile20UserService;

    @Autowired
    IamUserRepository iamUserRepository;

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
     * Profile 20 user created in tenantA is tenant-scoped:
     * - The user's tenant_id matches tenantA.
     * - Querying by tenantB email returns empty (RLS isolation).
     */
    @Test
    void profile20UserBelongsToCorrectTenantAndIsIsolatedFromOtherTenant() {
        // Arrange: set up tenantA with admin
        UUID tenantAId = insertTenant("t090-a", "Tenant A T090");
        UUID orgAId = insertOrganization(tenantAId, "T090001", "Org A T090");
        UUID locationAId = insertLocation(tenantAId, orgAId, "Location A T090");

        // Act: create profile 20 user via service
        var result = createProfile20UserService.create(
                tenantAId,
                orgAId,
                locationAId,
                "Dr. User20 T090",
                "user20@tenant-a.local",
                "12345678901",
                "S3nha@User20",
                "RN",
                UUID.randomUUID() /* adminUserId */);

        // Assert: user tenant_id is tenantA
        var createdUser = iamUserRepository.findByEmailAndTenantId("user20@tenant-a.local", tenantAId);
        assertThat(createdUser)
                .as("T090: profile 20 user must be found by tenantA")
                .isPresent();
        assertThat(createdUser.get().getProfile())
                .as("T090: created user must be profile 20")
                .isEqualTo(20);

        // Assert: tenantB cannot find the user (RLS isolation)
        UUID tenantBId = UUID.randomUUID(); // random other tenant, not inserted
        var crossTenantLookup = iamUserRepository.findByEmailAndTenantId("user20@tenant-a.local", tenantBId);
        assertThat(crossTenantLookup)
                .as("T090: profile 20 user from tenantA must NOT be visible from tenantB")
                .isEmpty();

        // Assert: returned result fields are populated
        assertThat(result.userId()).isNotNull();
        assertThat(result.practitionerId()).isNotNull();
        assertThat(result.practitionerRoleId()).isNotNull();
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
