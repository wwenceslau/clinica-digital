package com.clinicadigital.gateway.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
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
import javax.sql.DataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class QuotaEnforcementTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_quota_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final UUID NOISY_TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HEALTHY_TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void seedTenants() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate.execute("TRUNCATE TABLE tenants CASCADE");
        jdbcTemplate.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, " +
                "quota_requests_per_minute, quota_concurrency, quota_storage_mb, created_at, updated_at) VALUES " +
                "('" + NOISY_TENANT_ID + "', 'noisy', 'Noisy Tenant', 'active', 'basic', 5, 10, 1024, NOW(), NOW())," +
                "('" + HEALTHY_TENANT_ID + "', 'healthy', 'Healthy Tenant', 'active', 'basic', 5, 10, 1024, NOW(), NOW())");
    }

    @Test
    void sixthRequestMustReturn429AndNotAffectOtherTenant() {
        for (int i = 1; i <= 5; i++) {
            ResponseEntity<String> response = getTenant(NOISY_TENANT_ID);
            assertThat(response.getStatusCode())
                    .as("request %s for noisy tenant must stay within quota", i)
                    .isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<String> blocked = getTenant(NOISY_TENANT_ID);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getBody())
                .isNotNull()
                .contains("\"issue\"")
                .contains("\"throttled\"")
                .contains("tenant quota exceeded");

        ResponseEntity<String> healthy = getTenant(HEALTHY_TENANT_ID);
        assertThat(healthy.getStatusCode())
                .as("quota exhaustion must not degrade healthy tenants")
                .isEqualTo(HttpStatus.OK);
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