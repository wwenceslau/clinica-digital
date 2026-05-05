package com.clinicadigital.gateway.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T129 — Backend login performance test.
 *
 * <p>Validates that the {@code POST /api/auth/login} endpoint meets the
 * performance SLO defined in the Constitution:
 * <ul>
 *   <li><b>p95 latency &lt; 300 ms</b> under a baseline concurrent load of 20 threads.</li>
 *   <li>No request must complete with a 5xx status under normal load.</li>
 * </ul>
 *
 * <p>Measurement methodology:
 * <ol>
 *   <li>Seed one tenant with a valid admin user (argon2id hash, pre-computed).</li>
 *   <li>Send 100 login requests concurrently from 20 threads.</li>
 *   <li>Collect wall-clock duration per request.</li>
 *   <li>Compute p95 from collected durations and assert &lt; 300 ms.</li>
 * </ol>
 *
 * <p>Note: This test exercises the full application stack including Flyway migrations,
 * DB queries, and password verification. It is tagged {@code @Tag("performance")} so it
 * can be excluded from unit test CI runs and included selectively in performance CI jobs.
 *
 * Refs: SC-001 (login p95 < 300ms)
 */
@Tag("performance")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginPerformanceTest {

    private static final int TOTAL_REQUESTS = 100;
    private static final int THREAD_COUNT = 20;
    private static final long P95_THRESHOLD_MS = 300L;

    private static final String FHIR_ORG_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BREstabelecimentoSaude\"]";
    private static final String FHIR_PRACTITIONER_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRProfissionalSaude\"]";
    private static final String FHIR_ROLE_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRVinculoProfissionalEstabelecimento\"]";
    private static final String LOC_PROFILE =
            "[\"http://www.saude.gov.br/fhir/r4/StructureDefinition/BRUnidadeSaude\"]";

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("clinica_login_perf_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seedPerfFixtures() {
        jdbc.execute("TRUNCATE TABLE iam_sessions CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_auth_challenges CASCADE");
        jdbc.execute("TRUNCATE TABLE iam_users CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioner_roles CASCADE");
        jdbc.execute("TRUNCATE TABLE practitioners CASCADE");
        jdbc.execute("TRUNCATE TABLE locations CASCADE");
        jdbc.execute("TRUNCATE TABLE organizations CASCADE");

        UUID tenantId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID pracId   = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID locId    = UUID.fromString("20000000-0000-0000-0000-000000000003");
        UUID roleId   = UUID.fromString("20000000-0000-0000-0000-000000000004");
        UUID userId   = UUID.fromString("20000000-0000-0000-0000-000000000005");

        jdbc.update("""
                INSERT INTO organizations
                    (id, tenant_id, fhir_resource_id, fhir_meta_profile, fhir_identifier_json,
                     fhir_name, fhir_active, cnes, display_name, quota_tier, account_active,
                     created_at, updated_at)
                VALUES (?,?,?,?::jsonb,?::jsonb,?,true,?,?,?,true,NOW(),NOW())
                """,
                tenantId, tenantId,
                "org-perf-001",
                FHIR_ORG_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1290001\"}]",
                "Clinica Performance", "1290001", "Clinica Performance", "standard");

        jdbc.update("""
                INSERT INTO locations
                    (id, tenant_id, organization_id, fhir_resource_id, fhir_meta_profile,
                     fhir_identifier_json, fhir_name, fhir_status, fhir_mode,
                     display_name, account_active, created_at, updated_at)
                VALUES (?,?,?,?,?::jsonb,?::jsonb,?,?,?,?,true,NOW(),NOW())
                """,
                locId, tenantId, tenantId,
                "loc-perf-001",
                LOC_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cnes\",\"value\":\"1290001\"}]",
                "Unidade Perf", "active", "instance", "Unidade Perf");

        jdbc.update("""
                INSERT INTO practitioners
                    (id, tenant_id, fhir_resource_id, fhir_meta_profile,
                     fhir_identifier_json, fhir_name_json, fhir_active,
                     display_name, cpf_encrypted, encryption_key_version,
                     account_active, created_at, updated_at)
                VALUES (?,?,?,?::jsonb,?::jsonb,?::jsonb,true,?,decode('','hex'),'v1',true,NOW(),NOW())
                """,
                pracId, tenantId,
                "prac-perf-001",
                FHIR_PRACTITIONER_PROFILE,
                "[{\"system\":\"https://saude.gov.br/sid/cpf\",\"value\":\"00000000129\"}]",
                "[{\"use\":\"official\",\"text\":\"Admin Perf\"}]",
                "Admin Perf");

        jdbc.update("""
                INSERT INTO practitioner_roles
                    (id, tenant_id, organization_id, location_id, practitioner_id,
                     fhir_resource_id, fhir_meta_profile, role_code, active, primary_role,
                     created_at, updated_at)
                VALUES (?,?,?,?,?,?,?::jsonb,?,true,true,NOW(),NOW())
                """,
                roleId, tenantId, tenantId, locId, pracId,
                "role-perf-001",
                FHIR_ROLE_PROFILE,
                "ADMIN");

        // Password = "Strong!Pass1" — pre-hashed with bcrypt cost 10 for performance tests
        // (lower cost intentional in test fixture to avoid inflating latency measurements)
        jdbc.update("""
                INSERT INTO iam_users
                    (id, tenant_id, practitioner_id, email, password_hash, password_algo,
                     profile, account_active, failed_login_count, created_at, updated_at)
                VALUES (?,?,?,?,?,?,10,true,0,NOW(),NOW())
                """,
                userId, tenantId, pracId,
                "perf@clinica.com",
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
                "bcrypt");
    }

    // ── SC1: p95 of login under concurrent load must be < 300ms ────────────

    @Test
    void loginEndpoint_p95LatencyUnderConcurrentLoad_isBelowThreshold() throws Exception {
        String loginBody = """
                {"email":"perf@clinica.com","password":"Strong!Pass1"}
                """;

        List<Long> durations = collectLoginDurations(loginBody, TOTAL_REQUESTS, THREAD_COUNT);

        long p95 = computePercentile(durations, 95);
        System.out.printf(
                "[T129] Login p95 = %d ms over %d requests (%d threads). Threshold = %d ms%n",
                p95, TOTAL_REQUESTS, THREAD_COUNT, P95_THRESHOLD_MS);

        assertThat(p95)
                .as("Login p95 latency must be below %d ms (Constitution SC-001). Measured: %d ms",
                        P95_THRESHOLD_MS, p95)
                .isLessThanOrEqualTo(P95_THRESHOLD_MS);
    }

    // ── SC2: No 5xx errors under baseline load ───────────────────────────────

    @Test
    void loginEndpoint_noServerErrorsUnderBaselineLoad() throws Exception {
        String loginBody = """
                {"email":"perf@clinica.com","password":"Strong!Pass1"}
                """;

        List<Integer> statuses = collectLoginStatuses(loginBody, TOTAL_REQUESTS, THREAD_COUNT);
        long serverErrors = statuses.stream().filter(s -> s >= 500).count();

        assertThat(serverErrors)
                .as("No 5xx errors expected under baseline load of %d concurrent requests",
                        THREAD_COUNT)
                .isZero();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private List<Long> collectLoginDurations(String body, int total, int threads) throws Exception {
        List<Long> results = new ArrayList<>(total);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tasks.add(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                long start = System.currentTimeMillis();
                restTemplate.postForEntity("/api/auth/login",
                        new HttpEntity<>(body, headers), Map.class);
                return System.currentTimeMillis() - start;
            });
        }

        List<Future<Long>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        for (Future<Long> f : futures) {
            results.add(f.get());
        }
        return results;
    }

    private List<Integer> collectLoginStatuses(String body, int total, int threads) throws Exception {
        List<Integer> statuses = new ArrayList<>(total);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            tasks.add(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<Map> resp = restTemplate.postForEntity(
                        "/api/auth/login", new HttpEntity<>(body, headers), Map.class);
                return resp.getStatusCode().value();
            });
        }

        List<Future<Integer>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        for (Future<Integer> f : futures) {
            statuses.add(f.get());
        }
        return statuses;
    }

    private long computePercentile(List<Long> durations, int percentile) {
        List<Long> sorted = new ArrayList<>(durations);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.min(index, sorted.size() - 1));
    }
}
