package com.clinicadigital.shared.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * T030a - Unit tests for TenantJdbcContextInterceptor.
 * Written BEFORE implementation per Art. I (Test-First).
 * Refs: FR-016, FR-016a
 */
@ExtendWith(MockitoExtension.class)
class TenantJdbcContextInterceptorTest {

    @Mock
    private JdbcOperations jdbc;

    private TenantContextHolder holder;
    private TenantJdbcContextInterceptor interceptor;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
        interceptor = new TenantJdbcContextInterceptor(jdbc, holder);
    }

    @Test
    void shouldExecuteSetLocalWhenTenantContextIsPresent() {
        UUID tenantId = UUID.randomUUID();
        holder.set(TenantContext.from(tenantId));

        interceptor.applyTenantContext();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).execute(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("SET LOCAL app.tenant_id"),
                "SQL must contain SET LOCAL app.tenant_id but was: " + executedSql);
        assertTrue(executedSql.contains(tenantId.toString()),
                "SQL must contain the tenant UUID but was: " + executedSql);
    }

    @Test
    void shouldFailClosedWhenTenantContextIsAbsent() {
        // holder has no context — fail-closed per FR-016a
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> interceptor.applyTenantContext()
        );
        assertTrue(ex.getMessage().contains("TenantContextHolder"),
                "Exception message must reference TenantContextHolder but was: " + ex.getMessage());
        verifyNoInteractions(jdbc);
    }

    @Test
    void shouldNeverExecuteJdbcBeforeContextValidation() {
        assertThrows(IllegalStateException.class, () -> interceptor.applyTenantContext());
        verifyNoInteractions(jdbc);
    }

    @Test
    void shouldIncludeExactTenantIdInQuery() {
        UUID tenantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        holder.set(TenantContext.from(tenantId));

        interceptor.applyTenantContext();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).execute(sqlCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("550e8400-e29b-41d4-a716-446655440000"),
                "SQL must contain the literal UUID string");
    }
}
