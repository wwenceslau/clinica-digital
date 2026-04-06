package com.clinicadigital.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T043 — Integration test: verifies that a request without the X-Tenant-ID header
 * receives a 403 Forbidden response whose body conforms to the FHIR OperationOutcome
 * error structure (FR-002a, Art. VI).
 *
 * <p><b>TDD state</b>: RED until:
 * <ol>
 *   <li>{@code TenantContextFilter} is implemented (T048, Phase 3.C) — introduces the 403 response.</li>
 *   <li>{@code GlobalExceptionHandler} is implemented (T052, Phase 3.C) — adds the OperationOutcome body.</li>
 * </ol>
 * Current responses return 404 (no filter + no endpoints). Goes GREEN after Phase 3.C is complete.
 *
 * <p>Contract:
 * <ul>
 *   <li>HTTP status = 403 Forbidden.</li>
 *   <li>Response body is JSON and contains an {@code "issue"} array.</li>
 *   <li>Each issue has {@code "severity": "error"}, {@code "code": "forbidden"}.</li>
 *   <li>Body contains a {@code "diagnostics"} string describing the missing tenant context.</li>
 * </ul>
 *
 * Refs: FR-002a, Art. VI (FHIR OperationOutcome)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantMissingContextTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_missing_ctx_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    /**
     * FR-002a + Art. VI: A request without X-Tenant-ID MUST return 403 with a
     * JSON body containing the FHIR OperationOutcome structure.
     * RED: currently returns 404 (no filter). Goes GREEN with TenantContextFilter (T048).
     */
    @Test
    void missingTenantIdHeaderMustReturn403Forbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tenants", String.class);

        assertThat(response.getStatusCode())
                .as("FR-002a: missing X-Tenant-ID must return 403 Forbidden")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Art. VI: The 403 error response body MUST be JSON with a FHIR OperationOutcome {@code "issue"} array.
     * RED: currently returns 404 with no structured body. Goes GREEN with GlobalExceptionHandler (T052).
     */
    @Test
    void forbiddenResponseBodyMustContainFhirIssueArray() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tenants", String.class);

        assertThat(response.getStatusCode())
                .as("precondition: must be 403 before checking body")
                .isEqualTo(HttpStatus.FORBIDDEN);

        String body = response.getBody();
        assertThat(body)
                .as("Art. VI: 403 body must be FHIR OperationOutcome with 'issue' array")
                .isNotNull()
                .contains("\"issue\"");
    }

    /**
     * Art. VI: Each issue in the OperationOutcome MUST have {@code severity: "error"}
     * and {@code code: "forbidden"}.
     * RED: currently returns 404. Goes GREEN with TenantContextFilter + GlobalExceptionHandler.
     */
    @Test
    void forbiddenResponseIssueMustHaveSeverityErrorAndCodeForbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tenants", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String body = response.getBody();
        assertThat(body)
                .as("OperationOutcome issue must have severity=error and code=forbidden (Art. VI)")
                .contains("\"severity\"")
                .contains("\"error\"")
                .contains("\"code\"")
                .contains("\"forbidden\"");
    }

    /**
     * FR-002a: The diagnostics field MUST describe the missing tenant context to support
     * auditability of the rejection event.
     * RED: goes GREEN with GlobalExceptionHandler implementation (T052).
     */
    @Test
    void forbiddenResponseMustIncludeDiagnosticsAboutMissingTenantContext() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tenants", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String body = response.getBody();
        assertThat(body)
                .as("FR-002a: diagnostics must describe missing tenant context for auditability")
                .contains("\"diagnostics\"")
                .containsAnyOf("tenant", "X-Tenant-ID", "context");
    }

    /**
     * Verify that a request WITH a valid X-Tenant-ID header is NOT rejected with 403.
     * This ensures the filter allows valid requests to proceed.
     * RED: currently 404. After Phase 3.B/C, this returns 404 (no endpoint) NOT 403.
     */
    @Test
    void requestWithValidTenantIdMustNotBeRejectedBy403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode())
                .as("Valid X-Tenant-ID must not produce 403 (should get 404 = no endpoint, not boundary rejection)")
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }
}
