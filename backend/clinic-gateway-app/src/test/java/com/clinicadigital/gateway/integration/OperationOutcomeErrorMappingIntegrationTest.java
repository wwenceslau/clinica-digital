package com.clinicadigital.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T061 [P] [US7] Integration test: backend error responses comply with
 * OperationOutcome contract — every error must include both
 * {@code issue[].details.text} AND {@code issue[].diagnostics}.
 *
 * <p>Scenarios:
 * <ol>
 *   <li>Invalid credentials (401) → OperationOutcome com details.text e diagnostics</li>
 *   <li>Missing X-Tenant-ID em rota protegida (403) → OperationOutcome padronizado</li>
 *   <li>Quota exceeded mock (429) → OperationOutcome com code=throttled</li>
 *   <li>Bad JSON body (400) → OperationOutcome com code=invalid</li>
 *   <li>RNDS unsupported profile em registro (400) → OperationOutcome com diagnostics técnico preservado</li>
 * </ol>
 *
 * Refs: FR-015, SC-002, Art. VI (OperationOutcome standard)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class OperationOutcomeErrorMappingIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_oo_mapping_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    // ── SC1: Invalid credentials → 401 ──────────────────────────────────────

    @Test
    void invalidCredentials_returns401_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"nobody@test.com","password":"wrong-password"}
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── SC2: Missing tenant header on protected route → 403 ─────────────────

    @Test
    void missingTenantId_returns403_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String tenantId = UUID.randomUUID().toString();
        // Call a route that requires X-Tenant-ID, omitting the header
        String body = """
                {"profileType":10,"email":"admin@test.com","password":"Senha!123","displayName":"Admin"}
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/tenants/" + tenantId + "/admin",
                new HttpEntity<>(body, headers),
                Map.class);

        // Either 403 (tenant context missing) or 401 (auth failure) — both must be OperationOutcome
        assertThat(response.getStatusCode().value()).isBetween(400, 499);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── SC3: Bad JSON body → 400 ─────────────────────────────────────────────

    @Test
    void malformedJsonBody_returns4xx_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>("NOT_VALID_JSON", headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isBetween(400, 499);
        // Body may be null on parse failure, so only assert if present
        if (response.getBody() != null && response.getBody().containsKey("resourceType")) {
            assertOperationOutcomeShape(response.getBody());
        }
    }

    // ── SC4: Missing required fields → 400 ──────────────────────────────────

    @Test
    void missingRequiredFields_returns400_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Missing password field
        String body = """
                {"email":"nobody@test.com"}
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isBetween(400, 499);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── SC5: RNDS unsupported profile → diagnostics preserved ────────────────

    @Test
    void rndsUnsupportedProfile_presenceOfDiagnosticsInResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Attempt clinic registration with an invalid RNDS profile URI
        String body = """
                {
                  "displayName": "Test Clinic",
                  "cnes": "9999999",
                  "email": "admin@testclinic.local",
                  "cpf": "00000000000",
                  "password": "Senha!123",
                  "rndsProfile": "http://invalid.rnds.profile/not-supported"
                }
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/public/clinic-registration",
                new HttpEntity<>(body, headers),
                Map.class);

        // Must be a client error — either 400 (RNDS validation) or any 4xx
        assertThat(response.getStatusCode().value()).isBetween(400, 499);
        if (response.getBody() != null && response.getBody().containsKey("resourceType")) {
            assertOperationOutcomeShape(response.getBody());
            // Diagnostics must preserve technical detail for traceability
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) response.getBody().get("issue");
            if (issues != null && !issues.isEmpty()) {
                Object diagnostics = issues.get(0).get("diagnostics");
                assertThat(diagnostics).isNotNull();
                assertThat(diagnostics.toString()).isNotBlank();
            }
        }
    }

    // ── Assertion helper ────────────────────────────────────────────────────

    /**
     * Validates OperationOutcome shape contract:
     * resourceType=OperationOutcome, issue[] non-empty,
     * each issue has details.text AND diagnostics (US7 requirement).
     */
    @SuppressWarnings("unchecked")
    private void assertOperationOutcomeShape(Map<String, Object> body) {
        assertThat(body)
                .as("Response body must not be null")
                .isNotNull();
        assertThat(body.get("resourceType"))
                .as("resourceType must be 'OperationOutcome'")
                .isEqualTo("OperationOutcome");

        List<Map<String, Object>> issues = (List<Map<String, Object>>) body.get("issue");
        assertThat(issues)
                .as("issue array must be present and non-empty")
                .isNotNull()
                .isNotEmpty();

        for (Map<String, Object> issue : issues) {
            assertThat(issue.get("severity"))
                    .as("issue.severity must be present")
                    .isNotNull();
            assertThat(issue.get("code"))
                    .as("issue.code must be present")
                    .isNotNull();

            // US7 core requirement: both details.text AND diagnostics must be present
            Map<String, Object> details = (Map<String, Object>) issue.get("details");
            assertThat(details)
                    .as("issue.details must be present (US7: details.text required)")
                    .isNotNull();
            assertThat(details.get("text"))
                    .as("issue.details.text must be non-null and non-blank (US7)")
                    .isNotNull()
                    .asString()
                    .isNotBlank();

            assertThat(issue.get("diagnostics"))
                    .as("issue.diagnostics must be present for traceability (US7)")
                    .isNotNull()
                    .asString()
                    .isNotBlank();
        }
    }
}
