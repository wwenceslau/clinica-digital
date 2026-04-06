package com.clinicadigital.gateway.filters;

import com.clinicadigital.gateway.exception.InvalidTenantContextException;
import com.clinicadigital.gateway.exception.TenantContextMissingException;
import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantContextHolder tenantContextHolder;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public TenantContextFilter(TenantContextHolder tenantContextHolder,
                               @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.tenantContextHolder = tenantContextHolder;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/") || uri.startsWith("/tenants"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String rawTenantId = request.getHeader(TENANT_HEADER);
            if (rawTenantId == null || rawTenantId.isBlank()) {
                throw new TenantContextMissingException("tenant context missing: X-Tenant-ID header is required");
            }

            UUID tenantId;
            try {
                tenantId = UUID.fromString(rawTenantId);
            } catch (IllegalArgumentException ex) {
                throw new InvalidTenantContextException("tenant context invalid: X-Tenant-ID must be a UUID");
            }

            tenantContextHolder.set(TenantContext.from(tenantId));
            filterChain.doFilter(request, response);
        } catch (TenantContextMissingException | InvalidTenantContextException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            tenantContextHolder.clear();
        }
    }
}
