package com.clinicadigital.gateway.integration;

import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.tenant.application.QuotaService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T135: noisy-neighbor multi-tenant performance isolation validation.
 */
@SpringBootTest
@Testcontainers
class NoisyNeighborPerformanceIsolationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_noisy_neighbor_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final UUID TENANT_1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TENANT_3 = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID TENANT_4 = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID TENANT_5 = UUID.fromString("10000000-0000-0000-0000-000000000005");

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    QuotaService quotaService;

    @BeforeEach
    void seedFiveTenantsForPerformanceRun() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate.execute("TRUNCATE TABLE tenants CASCADE");
        jdbcTemplate.execute("INSERT INTO tenants (id, slug, legal_name, status, plan_tier, quota_requests_per_minute, quota_concurrency, quota_storage_mb, created_at, updated_at) VALUES " +
                "('" + TENANT_1 + "', 't1', 'Tenant 1', 'active', 'basic', 5000, 100, 1024, NOW(), NOW())," +
                "('" + TENANT_2 + "', 't2', 'Tenant 2', 'active', 'basic', 5000, 100, 1024, NOW(), NOW())," +
                "('" + TENANT_3 + "', 't3', 'Tenant 3', 'active', 'basic', 5000, 100, 1024, NOW(), NOW())," +
                "('" + TENANT_4 + "', 't4', 'Tenant 4', 'active', 'basic', 5000, 100, 1024, NOW(), NOW())," +
                "('" + TENANT_5 + "', 't5', 'Tenant 5', 'active', 'basic', 5000, 100, 1024, NOW(), NOW())");
    }

    @Test
    void healthyTenantsP95ShouldNotIncreaseMoreThanTenPercentDuringNoisySpike() throws Exception {
        List<UUID> healthyTenants = List.of(TENANT_2, TENANT_3, TENANT_4, TENANT_5);

        long baselineP95 = measureHealthyP95(healthyTenants, 200);
        long spikeP95 = measureWithNoisySpikeP95(TENANT_1, healthyTenants, 1000, 200);

        double allowedMax = (baselineP95 * 1.10d) + 250_000d;
        assertThat(spikeP95)
                .as("noisy-neighbor isolation: healthy tenant p95 must remain <= 10%% (+small jitter tolerance)")
                .isLessThanOrEqualTo((long) allowedMax);
    }

    private long measureHealthyP95(List<UUID> tenants, int requestsPerTenant) {
        List<Long> latencies = new ArrayList<>();
        for (UUID tenantId : tenants) {
            for (int i = 0; i < requestsPerTenant; i++) {
                TenantContextStore.set(TenantContext.from(tenantId));
                try {
                    long start = System.nanoTime();
                    quotaService.checkQuota(tenantId, QuotaService.HTTP_REQUEST_METRIC);
                    latencies.add(System.nanoTime() - start);
                } finally {
                    TenantContextStore.clear();
                }
            }
        }
        return p95(latencies);
    }

    private long measureWithNoisySpikeP95(UUID noisyTenant,
                                          List<UUID> healthyTenants,
                                          int noisyRequests,
                                          int healthyRequestsPerTenant) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<List<Long>>> tasks = new ArrayList<>();

            tasks.add(() -> {
                for (int i = 0; i < noisyRequests; i++) {
                    TenantContextStore.set(TenantContext.from(noisyTenant));
                    try {
                        quotaService.checkQuota(noisyTenant, QuotaService.HTTP_REQUEST_METRIC);
                    } finally {
                        TenantContextStore.clear();
                    }
                }
                return List.of();
            });

            for (UUID healthy : healthyTenants) {
                tasks.add(() -> {
                    List<Long> latencies = new ArrayList<>();
                    for (int i = 0; i < healthyRequestsPerTenant; i++) {
                        TenantContextStore.set(TenantContext.from(healthy));
                        try {
                            long start = System.nanoTime();
                            quotaService.checkQuota(healthy, QuotaService.HTTP_REQUEST_METRIC);
                            latencies.add(System.nanoTime() - start);
                        } finally {
                            TenantContextStore.clear();
                        }
                    }
                    return latencies;
                });
            }

            List<Future<List<Long>>> futures = executor.invokeAll(tasks);
            List<Long> healthyLatencies = new ArrayList<>();
            for (int i = 1; i < futures.size(); i++) {
                healthyLatencies.addAll(futures.get(i).get());
            }
            return p95(healthyLatencies);
        } finally {
            executor.shutdownNow();
        }
    }

    private long p95(List<Long> values) {
        if (values.isEmpty()) {
            return 0L;
        }
        values.sort(Long::compareTo);
        int index = (int) Math.ceil(values.size() * 0.95d) - 1;
        return values.get(Math.max(index, 0));
    }
}
