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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T049 — Contract test: POST /api/auth/select-organization (US4 challenge resolution).
 *
 * <p>Contract:
 * <ul>
 *   <li>POST with missing challengeToken → 400 Bad Request</li>
 *   <li>POST with missing organizationId → 400 Bad Request</li>
 *   <li>POST with invalid/expired challengeToken → 401 Unauthorized</li>
 *   <li>Endpoint does NOT require X-Tenant-ID header</li>
 * </ul>
 *
 * Refs: FR-006-US4, Art. VI (OperationOutcome)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class SelectOrganizationContractTest {

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_select_org_contract_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void selectOrgEndpointExistsWithoutTenantIdHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("""
                {"challengeToken":"invalid-token","organizationId":"%s"}
                """, UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/select-organization",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: /api/auth/select-organization must not return 404")
                .isNotEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode())
                .as("US4: /api/auth/select-organization must not require X-Tenant-ID → must not return 403")
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void selectOrgWithInvalidTokenMustReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("""
                {"challengeToken":"definitely-invalid-token","organizationId":"%s"}
                """, UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/select-organization",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: invalid challenge token must return 401")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void selectOrgWithMissingChallengeTokenMustReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("""
                {"organizationId":"%s"}
                """, UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/select-organization",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: missing challengeToken must return 400")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void selectOrgWithMissingOrganizationIdMustReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"challengeToken":"some-token"}
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/select-organization",
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .as("US4: missing organizationId must return 400")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
