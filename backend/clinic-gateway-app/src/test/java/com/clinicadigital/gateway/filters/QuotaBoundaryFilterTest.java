package com.clinicadigital.gateway.filters;

import com.clinicadigital.shared.api.TenantContext;
import com.clinicadigital.shared.api.TenantContextStore;
import com.clinicadigital.tenant.application.QuotaService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QuotaBoundaryFilterTest {

    private final QuotaService quotaService = mock(QuotaService.class);
    private final HandlerExceptionResolver exceptionResolver = mock(HandlerExceptionResolver.class);

    @AfterEach
    void tearDown() {
        TenantContextStore.clear();
    }

    @Test
    void shouldBypassQuotaForSystemTenant() throws ServletException, IOException {
        QuotaBoundaryFilter filter = newFilter();
        TenantContextStore.set(TenantContext.from(TenantContextFilter.SYSTEM_TENANT_ID));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(quotaService, never()).checkAndEnforceQuota(any(), eq(QuotaService.HTTP_REQUEST_METRIC));
    }

    @Test
    void shouldEnforceQuotaForRegularTenant() throws ServletException, IOException {
        QuotaBoundaryFilter filter = newFilter();
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        TenantContextStore.set(TenantContext.from(tenantId));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(quotaService, times(1)).checkAndEnforceQuota(tenantId, QuotaService.HTTP_REQUEST_METRIC);
    }

    @SuppressWarnings("unchecked")
    private QuotaBoundaryFilter newFilter() {
        ObjectProvider tenantContextHolderProvider = mock(ObjectProvider.class);
        return new QuotaBoundaryFilter(tenantContextHolderProvider, quotaService, exceptionResolver);
    }
}
