package com.clinicadigital.gateway.filters;

import com.clinicadigital.gateway.exception.TenantContextMissingException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextHolder;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.tenant.application.QuotaExceededException;
import com.clinicadigital.tenant.application.QuotaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class QuotaBoundaryFilter extends OncePerRequestFilter {

    private final ObjectProvider<TenantContextHolder> tenantContextHolderProvider;
    private final QuotaService quotaService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public QuotaBoundaryFilter(ObjectProvider<TenantContextHolder> tenantContextHolderProvider,
                               QuotaService quotaService,
                               @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.tenantContextHolderProvider = tenantContextHolderProvider;
        this.quotaService = quotaService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // US4: /api/auth/** — no tenant quota check at login time (tenant unknown)
        if (uri.startsWith("/api/auth/")) return true;
        // /api/admin/tenants — system-level endpoint; no per-tenant quota applies
        if (uri.startsWith("/api/admin/tenants")) return true;
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/tenants/create".equals(uri)) {
            return true;
        }
        return !(uri.startsWith("/api/") || uri.startsWith("/tenants") || uri.startsWith("/auth"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext tenantContext = TenantContextStore.get();
            if (tenantContext == null) {
                TenantContextHolder tenantContextHolder = tenantContextHolderProvider.getIfAvailable();
                if (tenantContextHolder != null && tenantContextHolder.isPresent()) {
                    tenantContext = tenantContextHolder.getRequired();
                }
            }
            if (tenantContext == null) {
                throw new TenantContextMissingException("tenant context missing before quota check");
            }
            quotaService.checkAndEnforceQuota(
                    tenantContext.tenantId(),
                    QuotaService.HTTP_REQUEST_METRIC);
            filterChain.doFilter(request, response);
        } catch (QuotaExceededException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }
}