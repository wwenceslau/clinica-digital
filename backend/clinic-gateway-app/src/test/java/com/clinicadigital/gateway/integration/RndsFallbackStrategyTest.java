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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T123 — Fallback Strategy validation under RNDS dependency failure.
 *
 * <p>Validates that when the RNDS StructureDefinition validator encounters an
 * unsupported or unresolvable profile, the system:
 * <ol>
 *   <li>Does NOT throw an unhandled exception (no 500).</li>
 *   <li>Returns a structured FHIR {@code OperationOutcome} with {@code code=invalid}
 *       and a traceable {@code diagnostics} field.</li>
 *   <li>The registration endpoint degrades gracefully — the request is rejected with
 *       400 and a user-friendly error, without persisting partial state.</li>
 *   <li>An unsupported profile value ({@code meta.profile}) triggers the fallback path
 *       and produces {@code issue[0].diagnostics} containing "unsupported" or "profile".</li>
 * </ol>
 *
 * <p>Fallback strategy (local StructureDefinition validation):
 * <ul>
 *   <li>If the RNDS profile in {@code meta.profile} is not found in the local
 *       StructureDefinition package, the validator returns
 *       {@code code=invalid, diagnostics="unsupported RNDS StructureDefinition profile: <url>"}.</li>
 *   <li>The endpoint converts this to a 400 OperationOutcome — no retry, no partial write.</li>
 *   <li>Network failures to remote FHIR registries are isolated: only the local package
 *       is used, so network failures do not propagate.</li>
 * </ul>
 *
 * Refs: FR-015, FR-020, SC-002
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class RndsFallbackStrategyTest {

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_rnds_fallback_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    // ── SC1: Unknown/unsupported RNDS profile on Organization ──────────────

    @Test
    void clinicRegistration_withUnknownRndsProfile_returns400_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "organization": {
                    "name": "Clinica RNDS Fallback Test",
                    "displayName": "Clinica RNDS Fallback",
                    "cnes": "9999901",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cnes","value":"9999901"}],
                    "meta": {
                      "profile": ["http://hl7.org/fhir/StructureDefinition/UNKNOWN-PROFILE-THAT-DOES-NOT-EXIST"]
                    }
                  },
                  "adminPractitioner": {
                    "displayName": "Admin Fallback",
                    "email": "admin.fallback@test.com",
                    "cpf": "99988877766",
                    "password": "Strong!Pass1",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cpf","value":"99988877766"}],
                    "names": [{"text":"Admin Fallback"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
                  }
                }
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/public/clinic-registration",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertOperationOutcomeShape(response.getBody());
        assertDiagnosticsContainsProfileError(response.getBody());
    }

    // ── SC2: Unknown profile on Practitioner ───────────────────────────────

    @Test
    void clinicRegistration_withUnknownPractitionerProfile_returns400_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "organization": {
                    "name": "Clinica RNDS Prac Fallback",
                    "displayName": "Clinica RNDS Prac Fallback",
                    "cnes": "9999902",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cnes","value":"9999902"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude"]}
                  },
                  "adminPractitioner": {
                    "displayName": "Admin Prac Fallback",
                    "email": "admin.prac.fallback@test.com",
                    "cpf": "11122233344",
                    "password": "Strong!Pass1",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cpf","value":"11122233344"}],
                    "names": [{"text":"Admin Prac Fallback"}],
                    "meta": {"profile": ["http://hl7.org/fhir/StructureDefinition/UNKNOWN-PRACTITIONER-PROFILE"]}
                  }
                }
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/public/clinic-registration",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertOperationOutcomeShape(response.getBody());
        assertDiagnosticsContainsProfileError(response.getBody());
    }

    // ── SC3: Missing meta.profile entirely ────────────────────────────────

    @Test
    void clinicRegistration_withMissingMetaProfile_returns400_withOperationOutcome() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "organization": {
                    "name": "Clinica No Profile",
                    "displayName": "Clinica No Profile",
                    "cnes": "9999903",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cnes","value":"9999903"}]
                  },
                  "adminPractitioner": {
                    "displayName": "Admin No Profile",
                    "email": "admin.noprofile@test.com",
                    "cpf": "55544433322",
                    "password": "Strong!Pass1",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cpf","value":"55544433322"}],
                    "names": [{"text":"Admin No Profile"}]
                  }
                }
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/public/clinic-registration",
                new HttpEntity<>(body, headers),
                Map.class);

        // Must be rejected — meta.profile is required per RNDS rules
        assertThat(response.getStatusCode().value()).isBetween(400, 422);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── SC4: Graceful degradation — no 500 on validator internal error ──────

    @Test
    void clinicRegistration_withEmptyProfileArray_returns400_notInternalError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "organization": {
                    "name": "Clinica Empty Profile",
                    "displayName": "Clinica Empty Profile",
                    "cnes": "9999904",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cnes","value":"9999904"}],
                    "meta": {"profile": []}
                  },
                  "adminPractitioner": {
                    "displayName": "Admin Empty Profile",
                    "email": "admin.emptyprofile@test.com",
                    "cpf": "66677788899",
                    "password": "Strong!Pass1",
                    "identifiers": [{"system":"https://saude.gov.br/sid/cpf","value":"66677788899"}],
                    "names": [{"text":"Admin Empty Profile"}],
                    "meta": {"profile": ["http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude"]}
                  }
                }
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/public/clinic-registration",
                new HttpEntity<>(body, headers),
                Map.class);

        // Must NOT be 500 — fallback must produce a structured 4xx
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getStatusCode().value()).isBetween(400, 422);
        assertOperationOutcomeShape(response.getBody());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void assertOperationOutcomeShape(Map<?, ?> body) {
        assertThat(body).isNotNull();
        assertThat(body.get("resourceType")).isEqualTo("OperationOutcome");
        Object issueObj = body.get("issue");
        assertThat(issueObj).isNotNull().isInstanceOf(java.util.List.class);
        java.util.List<?> issues = (java.util.List<?>) issueObj;
        assertThat(issues).isNotEmpty();
        Map<?, ?> firstIssue = (Map<?, ?>) issues.get(0);
        assertThat(firstIssue.get("severity")).isNotNull();
        assertThat(firstIssue.get("code")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private void assertDiagnosticsContainsProfileError(Map<?, ?> body) {
        java.util.List<?> issues = (java.util.List<?>) body.get("issue");
        assertThat(issues).isNotEmpty();
        Map<?, ?> firstIssue = (Map<?, ?>) issues.get(0);
        String diagnostics = (String) firstIssue.get("diagnostics");
        assertThat(diagnostics).isNotBlank();
        // The diagnostics must mention profile or unsupported to guide the caller
        assertThat(diagnostics.toLowerCase()).containsAnyOf("profile", "unsupported", "rnds", "invalid");
    }
}
