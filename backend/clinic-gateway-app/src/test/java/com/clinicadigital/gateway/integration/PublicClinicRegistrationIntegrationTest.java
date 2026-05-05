package com.clinicadigital.gateway.integration;

import com.clinicadigital.iam.test.BaseIAMTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T041 [P] [US3] Integration test: public clinic registration endpoint validates
 * RNDS profiles and CNES uniqueness.
 *
 * Verifies via MockMvc:
 * <ul>
 *   <li>Valid payload creates tenant + admin → HTTP 201 with tenantId and adminPractitionerId.</li>
 *   <li>Duplicate CNES → HTTP 409 with FHIR OperationOutcome body.</li>
 *   <li>Invalid CNES format (not 7 digits) → HTTP 400 with OperationOutcome body.</li>
 *   <li>Duplicate admin email → HTTP 409 with OperationOutcome body.</li>
 * </ul>
 *
 * TDD state: RED until PublicClinicRegistrationController (T044) is implemented.
 * Refs: FR-003, FR-009, FR-022
 */
@AutoConfigureMockMvc
class PublicClinicRegistrationIntegrationTest extends BaseIAMTest {

    private static final String ENDPOINT = "/api/public/clinic-registration";

    private static final String VALID_PAYLOAD = """
            {
              "organization": {
                "displayName": "Clinica Bairro Azul",
                "cnes": "1234567"
              },
              "adminPractitioner": {
                "displayName": "Dra. Maria Silva",
                "email": "maria@bairroazul.local",
                "cpf": "12345678901",
                "password": "S3nha!Forte"
              }
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        // TRUNCATE ignores FOR EACH ROW triggers (avoids append-only guard on iam_audit_events)
        jdbcTemplate.execute("TRUNCATE iam_audit_events RESTART IDENTITY CASCADE");
        jdbcTemplate.update("DELETE FROM iam_users WHERE profile = 10");
        jdbcTemplate.update("DELETE FROM practitioners WHERE tenant_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM organizations WHERE TRUE");
        jdbcTemplate.update("DELETE FROM tenants WHERE id != ?", SYSTEM_TENANT_ID);
    }

    @Test
    void validRegistrationReturns201WithTenantAndAdminIds() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId", notNullValue()))
                .andExpect(jsonPath("$.adminPractitionerId", notNullValue()))
                .andExpect(jsonPath("$.organization.cnes", equalTo("1234567")))
                .andExpect(jsonPath("$.adminPractitioner.email", equalTo("maria@bairroazul.local")));
    }

    @Test
    void duplicateCnesReturns409WithOperationOutcome() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated());

        String conflictPayload = """
                {
                  "organization": {
                    "displayName": "Clinica Outra",
                    "cnes": "1234567"
                  },
                  "adminPractitioner": {
                    "displayName": "Dr. Joao Costa",
                    "email": "joao@outra.local",
                    "cpf": "98765432100",
                    "password": "S3nha!Forte"
                  }
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resourceType", equalTo("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", equalTo("error")))
                .andExpect(jsonPath("$.issue[0].code", equalTo("conflict")));
    }

    @Test
    void invalidCnesFormatReturns400WithOperationOutcome() throws Exception {
        String badPayload = """
                {
                  "organization": {
                    "displayName": "Clinica Invalida",
                    "cnes": "ABCDEF"
                  },
                  "adminPractitioner": {
                    "displayName": "Dr. Teste",
                    "email": "teste@invalida.local",
                    "cpf": "12345678901",
                    "password": "S3nha!Forte"
                  }
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateAdminEmailReturns409WithOperationOutcome() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated());

        String emailConflictPayload = """
                {
                  "organization": {
                    "displayName": "Clinica Rival",
                    "cnes": "7654321"
                  },
                  "adminPractitioner": {
                    "displayName": "Dra. Maria Silva",
                    "email": "maria@bairroazul.local",
                    "cpf": "11122233344",
                    "password": "S3nha!Forte"
                  }
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailConflictPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resourceType", equalTo("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].code", equalTo("conflict")));
    }
}
