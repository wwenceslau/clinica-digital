package com.clinicadigital.gateway.contract;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T040 — Contract test: HTTP endpoint with mandatory X-Tenant-ID header (FR-002a).
 *
 * <p><b>TDD state</b>: RED until {@code TenantContextFilter} is implemented in Phase 3.C (T048).
 * Current application returns 404 for unknown endpoints; once the filter is registered,
 * requests without {@code X-Tenant-ID} will be intercepted at the boundary and return 403.
 *
 * <p>Contract (FR-002a):
 * <ul>
 *   <li>Any request to the API without {@code X-Tenant-ID} header MUST be rejected with 403.</li>
 *   <li>Any request with a non-UUID {@code X-Tenant-ID} value MUST be rejected with 403.</li>
 *   <li>A request with a valid UUID {@code X-Tenant-ID} MUST NOT return 403.</li>
 * </ul>
 *
 * Refs: FR-002a, Art. 0
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantBoundaryContractTest {

        private static final String TRACE_HEADER = "X-Trace-ID";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_gateway_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    /**
     * FR-002a: Absent X-Tenant-ID header MUST produce 403 Forbidden at the boundary.
     * RED: currently returns 404 (no filter intercepting). Goes GREEN once TenantContextFilter is added (T048).
     */
    @Test
    void requestWithoutTenantIdHeaderMustBeForbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/tenants", String.class);

        assertThat(response.getStatusCode())
                .as("FR-002a: missing X-Tenant-ID must be rejected with 403 at boundary")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * FR-002a: Non-UUID value for X-Tenant-ID MUST produce 403 Forbidden.
     * RED: currently returns 404 (no filter). Goes GREEN once TenantContextFilter validates format.
     */
    @Test
    void requestWithInvalidUuidTenantIdMustBeForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "not-a-valid-uuid");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode())
                .as("FR-002a: invalid UUID X-Tenant-ID must be rejected with 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * FR-002a (inverse): A valid UUID X-Tenant-ID header MUST NOT result in 403.
     * The request may still fail (404 = no endpoint yet) but NOT due to boundary rejection.
     * RED: currently returns 404. After Phase 3.B/C this test will pass with 200 or 404 (not 403).
     */
    @Test
    void requestWithValidTenantIdHeaderMustNotBeForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode())
                .as("FR-002a: valid X-Tenant-ID must NOT be rejected at boundary")
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * FR-002a: Empty string X-Tenant-ID value MUST be treated as absent and return 403.
     * RED: currently returns 404. Goes GREEN once TenantContextFilter checks for blank values.
     */
    @Test
    void requestWithEmptyTenantIdHeaderMustBeForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode())
                .as("FR-002a: empty X-Tenant-ID must be rejected with 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requestWithoutTraceIdMustReceiveGeneratedTraceHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getHeaders().getFirst(TRACE_HEADER))
                .as("FR-010a: boundary must generate X-Trace-ID when absent")
                .isNotBlank()
                .startsWith("trace-");
    }

    @Test
    void requestWithValidTraceIdMustPreserveResponseHeader() {
        String traceId = "trace-550e8400-e29b-41d4-a716-446655440000";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", UUID.randomUUID().toString());
        headers.set(TRACE_HEADER, traceId);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenants", HttpMethod.GET, request, String.class);

        assertThat(response.getHeaders().getFirst(TRACE_HEADER))
                .as("FR-010a: boundary must preserve a valid incoming X-Trace-ID")
                .isEqualTo(traceId);
    }
}
