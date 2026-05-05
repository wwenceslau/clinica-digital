package com.clinicadigital.gateway.filters;

import com.clinicadigital.gateway.exception.InvalidTenantContextException;
import com.clinicadigital.gateway.exception.TenantContextMissingException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextHolder;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.shared.api.TraceContext;
import com.clinicadigital.shared.api.TraceContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TRACE_HEADER = "X-Trace-ID";

    /**
     * System tenant UUID used for public (unauthenticated) endpoints such as
     * {@code /api/public/**} and system-level admin endpoints such as
     * {@code /api/admin/tenants}. These endpoints do not carry a {@code X-Tenant-ID}
     * header because they create a new tenant or operate across all tenants.
     *
     * <p>Exposed as package-visible so {@link AuthenticationFilter} can detect
     * system-tenant context and pass {@code null} to {@link
     * com.clinicadigital.iam.application.SessionManager#validateSession} — since
     * super-user sessions are stored with {@code tenantId = null}.
     */
    static final UUID SYSTEM_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ObjectProvider<TenantContextHolder> tenantContextHolderProvider;
    private final ObjectProvider<TraceContextHolder> traceContextHolderProvider;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public TenantContextFilter(ObjectProvider<TenantContextHolder> tenantContextHolderProvider,
                               ObjectProvider<TraceContextHolder> traceContextHolderProvider,
                               @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.tenantContextHolderProvider = tenantContextHolderProvider;
        this.traceContextHolderProvider = traceContextHolderProvider;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // US4: /api/auth/** — tenant is unknown at login time; skip filter entirely
        if (uri.startsWith("/api/auth/")) return true;
        return !(uri.startsWith("/api/") || uri.startsWith("/tenants") || uri.startsWith("/auth"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        TenantContextHolder tenantContextHolder = tenantContextHolderProvider.getIfAvailable();
        TraceContextHolder traceContextHolder = traceContextHolderProvider.getIfAvailable();
        try {
            TraceContext traceContext = resolveTraceContext(request);
            if (traceContextHolder != null) {
                traceContextHolder.set(traceContext);
            }
            response.setHeader(TRACE_HEADER, traceContext.traceId());
            MDC.put("trace_id", traceContext.traceId());

            UUID tenantId;
            if (isPublicPath(request.getRequestURI())) {
                // Public endpoints (/api/public/**) operate under the system tenant context.
                // They do not carry an X-Tenant-ID header because they create new tenants.
                tenantId = SYSTEM_TENANT_ID;
            } else {
                String rawTenantId = request.getHeader(TENANT_HEADER);
                if (rawTenantId == null || rawTenantId.isBlank()) {
                    throw new TenantContextMissingException("tenant context missing: X-Tenant-ID header is required");
                }
                try {
                    tenantId = UUID.fromString(rawTenantId);
                } catch (IllegalArgumentException ex) {
                    throw new InvalidTenantContextException("tenant context invalid: X-Tenant-ID must be a UUID");
                }
            }

            TenantContext tenantContext = TenantContext.from(tenantId);
            TenantContextStore.set(tenantContext);
            if (tenantContextHolder != null) {
                tenantContextHolder.set(tenantContext);
            }
            MDC.put("tenant_id", tenantId.toString());
            filterChain.doFilter(request, response);
        } catch (TenantContextMissingException | InvalidTenantContextException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            if (tenantContextHolder != null) {
                tenantContextHolder.clear();
            }
            if (traceContextHolder != null) {
                traceContextHolder.clear();
            }
            TenantContextStore.clear();
            MDC.clear();
        }
    }

    private static boolean isPublicPath(String uri) {
        if (uri == null) return false;
        // /api/public/** — tenant registration and public clinic endpoints
        // /api/admin/tenants — super-user tenant provisioning endpoint; operates across all
        //   tenants at system level. Authentication is still enforced by AuthenticationFilter.
        return uri.startsWith("/api/public/") || uri.startsWith("/api/admin/tenants");
    }

    private TraceContext resolveTraceContext(HttpServletRequest request) {
        String incomingTraceId = request.getHeader(TRACE_HEADER);
        if (TraceContext.isValid(incomingTraceId)) {
            return TraceContext.from(incomingTraceId);
        }
        return TraceContext.generate();
    }
}
