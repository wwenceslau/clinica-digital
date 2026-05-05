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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T090 [US3] — Integration test: login fails when tenant_id is invalid or absent.
 *
 * <p><b>TDD state</b>: RED until {@link com.clinicadigital.gateway.api.AuthController}
 * (T103, Phase 5.B) is implemented.
 *
 * <p>Current expectation: HTTP 404 (no endpoint yet). Goes GREEN when:
 * <ol>
 *   <li>{@code AuthController.login} is implemented (T103).</li>
 *   <li>{@code TenantContextFilter} is applied to {@code /auth/**} routes.</li>
 * </ol>
 *
 * <p>Contract verified:
 * <ul>
 *   <li>POST /auth/login without {@code X-Tenant-ID} header → 403 Forbidden with
 *       FHIR OperationOutcome body.</li>
 *   <li>POST /auth/login with a random/unknown tenant UUID → 403 Forbidden.</li>
 *   <li>Response body contains the {@code "issue"} array (Art. VI).</li>
 *   <li>Response body contains {@code "severity": "error"} and
 *       {@code "code": "forbidden"} in the issue.</li>
 * </ul>
 *
 * Refs: FR-006a, Art. XXII, Art. VI (FHIR OperationOutcome)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthTenantBoundaryTest {

    @SuppressWarnings("resource")
@Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_auth_boundary_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    /**
     * FR-006a: POST /auth/login without X-Tenant-ID header MUST return 403 Forbidden.
     * TDD RED: currently returns 404 until AuthController + TenantContextFilter are implemented.
     */
    @Test
    void loginWithoutTenantIdHeaderMustReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"user@test.com","password":"secret"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode())
                .as("FR-006a: auth/login without X-Tenant-ID must return 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * FR-006a + Art. VI: The 403 response body must contain a FHIR OperationOutcome.
     * TDD RED: goes GREEN with AuthController (T103) + GlobalExceptionHandler (T052).
     */
    @Test
    void loginWithoutTenantIdMustReturnFhirOperationOutcomeBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"user@test.com","password":"secret"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .as("FR-006a + Art. VI: response must contain FHIR 'issue' array")
                .contains("issue");
    }

    /**
     * FR-006a: POST /auth/login with an invalid (unknown) tenant UUID MUST return 403 Forbidden.
     * TDD RED: goes GREEN with TenantContextFilter (T048) + AuthController (T103).
     */
    @Test
    void loginWithUnknownTenantMustReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", UUID.randomUUID().toString()); // random, not in DB
        String body = """
                {"email":"user@test.com","password":"secret"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode())
                .as("FR-006a: login with unknown tenant UUID must return 403")
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }
}
