package com.clinicadigital.tenant.application;

import com.clinicadigital.tenant.domain.ITenantRepository;
import com.clinicadigital.tenant.domain.Tenant;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class QuotaService {

    public static final String HTTP_REQUEST_METRIC = "http.request";
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(1);

    private final ITenantRepository tenantRepository;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentMap<QuotaKey, WindowCounter> counters = new ConcurrentHashMap<>();

    public QuotaService(ITenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void checkAndEnforceQuota(UUID tenantId, String metric) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (!HTTP_REQUEST_METRIC.equals(metric)) {
            throw new IllegalArgumentException("Unsupported quota metric: " + metric);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        int limit = tenant.getQuotaRequestsPerMinute();
        long nowMillis = clock.millis();
        QuotaKey key = new QuotaKey(tenantId, metric);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(nowMillis));

        int currentCount = counter.incrementWithinWindow(nowMillis, REQUEST_WINDOW.toMillis());
        if (currentCount > limit) {
            throw new QuotaExceededException(tenantId, metric, limit);
        }
    }

    public QuotaCheckResult checkQuota(UUID tenantId, String metric) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (!HTTP_REQUEST_METRIC.equals(metric)) {
            throw new IllegalArgumentException("Unsupported quota metric: " + metric);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        int limit = tenant.getQuotaRequestsPerMinute();
        long nowMillis = clock.millis();
        WindowCounter counter = counters.get(new QuotaKey(tenantId, metric));
        int used = counter == null ? 0 : counter.currentCountWithinWindow(nowMillis, REQUEST_WINDOW.toMillis());
        int remaining = Math.max(0, limit - used);

        return new QuotaCheckResult(tenantId, metric, limit, used, remaining, remaining > 0);
    }

    public record QuotaCheckResult(UUID tenantId, String metric, int limit, int used, int remaining, boolean allowed) {
    }

    private record QuotaKey(UUID tenantId, String metric) {
    }

    private static final class WindowCounter {
        private long windowStartMillis;
        private int count;

        private WindowCounter(long nowMillis) {
            this.windowStartMillis = nowMillis;
            this.count = 0;
        }

        private synchronized int incrementWithinWindow(long nowMillis, long windowMillis) {
            if (nowMillis - windowStartMillis >= windowMillis) {
                windowStartMillis = nowMillis;
                count = 0;
            }
            count++;
            return count;
        }

        private synchronized int currentCountWithinWindow(long nowMillis, long windowMillis) {
            if (nowMillis - windowStartMillis >= windowMillis) {
                windowStartMillis = nowMillis;
                count = 0;
            }
            return count;
        }
    }
}