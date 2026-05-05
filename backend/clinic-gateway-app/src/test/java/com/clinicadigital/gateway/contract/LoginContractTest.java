package com.clinicadigital.gateway.contract;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T048 — Contract test: POST /api/auth/login (US4 multi-org login).
 *
 * <p>Contract:
 * <ul>
 *   <li>POST with no body → 400 Bad Request</li>
 *   <li>POST with missing email field → 400 Bad Request</li>
 *   <li>POST with missing password field → 400 Bad Request</li>
 *   <li>POST with valid JSON but wrong credentials → 401 Unauthorized</li>
 *   <li>Endpoint does NOT require X-Tenant-ID header</li>
 * </ul>
 *
 * Refs: FR-006-US4, Art. VI (OperationOutcome)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginContractTest {

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_login_contract_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void loginEndpointExistsWithoutTenantIdHeader() {
        // Endpoint must be reachable without X-Tenant-ID (unlike /auth/login)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"nobody@test.com","password":"wrong-password"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);

        // Must NOT be 404 (endpoint exists) and must NOT be 403 (no tenant required)
        assertThat(response.getStatusCode())
                .as("US4: /api/auth/login must not return 404 — endpoint must exist")
                .isNotEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode())
                .as("US4: /api/auth/login must not require X-Tenant-ID → must not return 403")
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void loginWithInvalidCredentialsMustReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"nobody@test.com","password":"wrong-password"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: invalid credentials must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithMissingEmailMustReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"password":"some-password"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: missing email must return 400")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loginWithMissingPasswordMustReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"user@test.com"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: missing password must return 400")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
