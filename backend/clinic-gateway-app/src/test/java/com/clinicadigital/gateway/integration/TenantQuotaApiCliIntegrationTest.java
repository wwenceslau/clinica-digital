package com.clinicadigital.gateway.integration;

import com.clinicadigital.tenant.application.QuotaService;
import com.clinicadigital.tenant.cli.QuotaCommands;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T131: validates quota/rate limiting contract for authenticated API and CLI flows.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantQuotaApiCliIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_quota_api_cli_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @Autowired
    QuotaCommands quotaCommands;

    @Autowired
    QuotaService quotaService;

    @BeforeEach
    void seedTenants() throws Exception {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate.execute("TRUNCATE TABLE tenants CASCADE");
        jdbcTemplate.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, quota_requests_per_minute, quota_concurrency, quota_storage_mb, created_at, updated_at) VALUES " +
                "('" + TENANT_A + "', 'tenant-a', 'Tenant A', 'active', 'basic', 5, 10, 1024, NOW(), NOW())," +
                "('" + TENANT_B + "', 'tenant-b', 'Tenant B', 'active', 'basic', 5, 10, 1024, NOW(), NOW())");

        Field countersField = QuotaService.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        QuotaService targetQuotaService = AopTestUtils.getTargetObject(quotaService);
        ((ConcurrentMap<?, ?>) countersField.get(targetQuotaService)).clear();
    }

    @Test
    void apiShouldAllowRequestsWithinTenantQuota() {
        ResponseEntity<String> response = getTenant(TENANT_A);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void apiShouldReturn429WithOperationOutcomeWhenQuotaExceeded() {
        for (int i = 0; i < 5; i++) {
            assertThat(getTenant(TENANT_A).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<String> blocked = getTenant(TENANT_A);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getBody())
                .isNotNull()
                .contains("\"issue\"")
                .contains("\"throttled\"")
                .contains("tenant quota exceeded");
    }

    @Test
    void apiQuotaExhaustionMustNotAffectOtherTenant() {
        for (int i = 0; i < 6; i++) {
            getTenant(TENANT_A);
        }

        ResponseEntity<String> healthy = getTenant(TENANT_B);
        assertThat(healthy.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cliQuotaCheckShouldReturnDeterministicJsonSuccessContract() {
        String output = quotaCommands.check(TENANT_A, true);

        assertThat(output)
                .contains("\"tenant_id\"")
                .contains("\"operation\": \"quota.check\"")
                .contains("\"outcome\": \"success\"")
                .contains("\"trace_id\"");
    }

    @Test
    void cliQuotaCheckShouldReturnDeterministicJsonFailureContractForUnknownTenant() {
        String output = quotaCommands.check(UUID.randomUUID(), true);

        assertThat(output)
                .contains("\"issue\"")
                .contains("\"severity\": \"error\"")
                .contains("\"operation\": \"quota.check\"")
                .contains("\"outcome\": \"failure\"");
    }

    private ResponseEntity<String> getTenant(UUID tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId.toString());
        return restTemplate.exchange(
                "/tenants/" + tenantId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
