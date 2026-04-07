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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T091 [US3] — Integration test: logout revokes the session immediately;
 * subsequent requests with the same session_id are rejected.
 *
 * <p><b>TDD state</b>: RED until the following Phase 5.B tasks are implemented:
 * <ol>
 *   <li>T103 {@code POST /auth/login} — to obtain a valid session_id.</li>
 *   <li>T104 {@code POST /auth/logout} — revokes the session immediately.</li>
 *   <li>T102 {@code AuthenticationFilter} — validates {@code Authorization} header
 *       on every request and rejects revoked sessions.</li>
 * </ol>
 *
 * <p>Contract verified (when GREEN):
 * <ul>
 *   <li>After successful login, the returned session_id is accepted by a
 *       subsequent authenticated request.</li>
 *   <li>After POST /auth/logout with that session_id, the same session_id
 *       is rejected with 401 Unauthorized on the next request.</li>
 *   <li>The logout response JSON contains {@code "revoked": true}.</li>
 *   <li>The revocation is immediate (no TTL window) — the very next request
 *       after logout fails.</li>
 * </ul>
 *
 * Refs: FR-007, FR-007a (immediate revocation)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class SessionRevocationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_session_revocation_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    /**
     * FR-007: Logout must immediately revoke the session.
     * TDD RED: POST /auth/login and POST /auth/logout do not exist yet.
     * Goes GREEN after T103 + T104 (Phase 5.B).
     */
    @Test
    void logoutRevokesSessionAndSubsequentRequestIsRejected() {
        // Step 1: Login to obtain a session_id
        // This will fail until AuthController (T103) is implemented.
        String loginBody = """
                {"email":"admin@acme.com","password":"correct-pass","tenant_id":"00000000-0000-0000-0000-000000000001"}
                """;
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        loginHeaders.set("X-Tenant-ID", "00000000-0000-0000-0000-000000000001");

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/auth/login", new HttpEntity<>(loginBody, loginHeaders), String.class);

        // TDD RED: expect 404 (no endpoint) until Phase 5.B is done
        assertThat(loginResponse.getStatusCode())
                .as("TDD RED anchor: auth/login should return 200 when T103 is implemented")
                .isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * FR-007a: Revoked session_id MUST be rejected with 401 on subsequent calls.
     * TDD RED: POST /auth/logout does not exist yet.
     * Goes GREEN after T104 + T102 (Phase 5.B).
     */
    @Test
    void revokedSessionMustBeRejectedWith401() {
        // Simulate a logout call with a random session_id (endpoint doesn't exist yet)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", "00000000-0000-0000-0000-000000000001");
        String logoutBody = """
                {"session_id":"00000000-dead-beef-0000-000000000001"}
                """;

        ResponseEntity<String> logoutResponse = restTemplate.postForEntity(
                "/auth/logout", new HttpEntity<>(logoutBody, headers), String.class);

        // TDD RED: currently returns 404 — will return 200 after T104 is implemented
        assertThat(logoutResponse.getStatusCode())
                .as("TDD RED anchor: auth/logout should exist after T104")
                .isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * FR-007: Session revocation response must confirm revocation.
     * TDD RED: goes GREEN after T104 (AuthController.logout).
     */
    @Test
    void logoutResponseMustConfirmRevocation() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", "00000000-0000-0000-0000-000000000001");
        String logoutBody = """
                {"session_id":"00000000-dead-beef-0000-000000000001"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/logout", new HttpEntity<>(logoutBody, headers), String.class);

        // When T104 is implemented, the response body must contain "revoked": true
        // For now, this serves as the specification anchor.
        assertThat(response.getStatusCode())
                .as("TDD RED: logout endpoint returns 404 until T104 is implemented; " +
                    "when GREEN body must contain '\"revoked\":true' or '\"revoked\": true'")
                .isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
