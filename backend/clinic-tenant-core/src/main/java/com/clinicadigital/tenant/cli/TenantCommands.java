package com.clinicadigital.tenant.cli;

import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.shared.api.TraceContext;
import com.clinicadigital.tenant.application.TenantService;
import com.clinicadigital.tenant.domain.Tenant;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.UUID;

/**
 * CLI commands for tenant administration via Spring Shell.
 *
 * <p>Implements the {@code tenant create} and {@code tenant list} commands
 * per the CLI contract defined in {@code contracts/cli-contracts.md}
 * (FR-004, Art. II).
 *
 * <p>Admin CLI context: each command sets a tenant context in
 * {@link TenantContextStore} (thread-local) so that the
 * {@code TenantJdbcContextInterceptor} can propagate {@code SET LOCAL
 * app.tenant_id} even when there is no active HTTP request scope.
 */
@ShellComponent
public class TenantCommands {

    private final TenantService tenantService;

    public TenantCommands(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Creates a new tenant.
     *
     * <p>Usage: {@code tenant create --slug acme --legal-name "Acme Corp" --plan-tier basic [--json]}
     *
     * @param slug      unique tenant slug (required)
     * @param legalName legal name of the organisation (required)
     * @param planTier  subscription plan tier (required)
     * @param json      when {@code true}, emit structured JSON per CLI contract
     * @return formatted outcome string
     */
    @ShellMethod(key = "tenant create", value = "Create a new tenant")
    public String create(
            @ShellOption(value = "--slug", help = "Unique tenant slug (required)") String slug,
            @ShellOption(value = "--legal-name", help = "Legal name of the tenant (required)") String legalName,
            @ShellOption(value = "--plan-tier", help = "Subscription plan tier (required)") String planTier,
            @ShellOption(value = "--json", help = "Output as structured JSON", defaultValue = "false") boolean json
    ) {
        String traceId = TraceContext.generate().traceId();
        // Generate the tenant id upfront so the JDBC context matches the row being inserted.
        UUID newTenantId = UUID.randomUUID();
        TenantContextStore.set(TenantContext.from(newTenantId));
        try {
            Tenant tenant = tenantService.createTenant(slug, legalName, planTier);
            if (json) {
                return "{\n" +
                       "  \"tenant_id\": \"" + tenant.getId() + "\",\n" +
                       "  \"slug\": \"" + tenant.getSlug() + "\",\n" +
                       "  \"status\": \"" + tenant.getStatus() + "\",\n" +
                       "  \"trace_id\": \"" + traceId + "\",\n" +
                       "  \"operation\": \"tenant.create\",\n" +
                       "  \"outcome\": \"success\"\n" +
                       "}";
            }
            return "Created tenant: " + tenant.getSlug() + " (id=" + tenant.getId() + ")";
        } catch (Exception ex) {
            if (json) {
                return buildErrorJson(sanitize(ex.getMessage()), traceId, "tenant.create");
            }
            return "Error: " + ex.getMessage();
        } finally {
            TenantContextStore.clear();
        }
    }

    /**
     * Lists all tenants visible in the current admin context.
     *
     * <p>Usage: {@code tenant list [--json]}
     *
     * @param json when {@code true}, emit structured JSON per CLI contract
     * @return formatted outcome string
     */
    @ShellMethod(key = "tenant list", value = "List tenants")
    public String list(
            @ShellOption(value = "--json", help = "Output as structured JSON", defaultValue = "false") boolean json
    ) {
        String traceId = TraceContext.generate().traceId();
        // Use a well-known system UUID as admin sentinel for the JDBC context.
        // Full cross-tenant admin requires a BYPASSRLS database role (future phase).
        TenantContextStore.set(TenantContext.from(UUID.fromString("00000000-0000-0000-0000-000000000001")));
        try {
            List<Tenant> tenants = tenantService.listTenants();
            if (json) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\n  \"tenants\": [");
                for (int i = 0; i < tenants.size(); i++) {
                    Tenant t = tenants.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("\n    {\n")
                      .append("      \"tenant_id\": \"").append(t.getId()).append("\",\n")
                      .append("      \"slug\": \"").append(t.getSlug()).append("\",\n")
                      .append("      \"status\": \"").append(t.getStatus()).append("\",\n")
                      .append("      \"plan_tier\": \"").append(t.getPlanTier()).append("\"\n")
                      .append("    }");
                }
                sb.append("\n  ],\n")
                  .append("  \"trace_id\": \"").append(traceId).append("\",\n")
                  .append("  \"operation\": \"tenant.list\",\n")
                  .append("  \"outcome\": \"success\"\n")
                  .append("}");
                return sb.toString();
            }
            if (tenants.isEmpty()) {
                return "No tenants found.";
            }
            StringBuilder sb = new StringBuilder();
            for (Tenant t : tenants) {
                sb.append(t.getSlug()).append(" (").append(t.getId()).append(")\n");
            }
            return sb.toString().trim();
        } catch (Exception ex) {
            if (json) {
                return buildErrorJson(sanitize(ex.getMessage()), traceId, "tenant.list");
            }
            return "Error: " + ex.getMessage();
        } finally {
            TenantContextStore.clear();
        }
    }

    private static String buildErrorJson(String diagnostics, String traceId, String operation) {
        return "{\n" +
               "  \"issue\": [\n" +
               "    {\n" +
               "      \"severity\": \"error\",\n" +
               "      \"code\": \"forbidden\",\n" +
               "      \"diagnostics\": \"" + diagnostics + "\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"trace_id\": \"" + traceId + "\",\n" +
               "  \"operation\": \"" + operation + "\",\n" +
               "  \"outcome\": \"failure\"\n" +
               "}";
    }

    private static String sanitize(String msg) {
        if (msg == null) return "unknown error";
        return msg.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
