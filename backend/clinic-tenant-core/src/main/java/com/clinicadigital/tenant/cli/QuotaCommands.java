package com.clinicadigital.tenant.cli;

import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.shared.api.TraceContext;
import com.clinicadigital.tenant.application.QuotaService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.UUID;

/**
 * CLI commands for quota operations.
 */
@ShellComponent
public class QuotaCommands {

    private final QuotaService quotaService;

    public QuotaCommands(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @ShellMethod(key = "quota check", value = "Check current quota status for a tenant")
    public String check(
            @ShellOption(value = "--tenant-id", help = "Tenant id (required)") UUID tenantId,
            @ShellOption(value = "--json", help = "Output as structured JSON", defaultValue = "false") boolean json
    ) {
        String traceId = TraceContext.generate().traceId();
        TenantContextStore.set(TenantContext.from(tenantId));
        try {
            QuotaService.QuotaCheckResult result = quotaService.checkQuota(tenantId, QuotaService.HTTP_REQUEST_METRIC);
            if (json) {
                return "{\n" +
                       "  \"tenant_id\": \"" + result.tenantId() + "\",\n" +
                       "  \"metric\": \"" + result.metric() + "\",\n" +
                       "  \"limit\": " + result.limit() + ",\n" +
                       "  \"used\": " + result.used() + ",\n" +
                       "  \"remaining\": " + result.remaining() + ",\n" +
                       "  \"allowed\": " + result.allowed() + ",\n" +
                       "  \"trace_id\": \"" + traceId + "\",\n" +
                       "  \"operation\": \"quota.check\",\n" +
                       "  \"outcome\": \"success\"\n" +
                       "}";
            }

            return "Quota for " + result.tenantId() + ": " + result.used() + "/" + result.limit() +
                   " used (remaining=" + result.remaining() + ")";
        } catch (Exception ex) {
            if (json) {
                return "{\n" +
                       "  \"issue\": [\n" +
                       "    {\n" +
                       "      \"severity\": \"error\",\n" +
                       "      \"code\": \"forbidden\",\n" +
                       "      \"diagnostics\": \"" + sanitize(ex.getMessage()) + "\"\n" +
                       "    }\n" +
                       "  ],\n" +
                       "  \"trace_id\": \"" + traceId + "\",\n" +
                       "  \"operation\": \"quota.check\",\n" +
                       "  \"outcome\": \"failure\"\n" +
                       "}";
            }
            return "Error: " + ex.getMessage();
        } finally {
            TenantContextStore.clear();
        }
    }

    private static String sanitize(String msg) {
        if (msg == null) {
            return "unknown error";
        }
        return msg.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}