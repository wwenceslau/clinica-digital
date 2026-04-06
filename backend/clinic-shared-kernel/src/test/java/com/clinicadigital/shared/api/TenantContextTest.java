package com.clinicadigital.shared.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @Test
    void shouldRejectNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new TenantContext(null));
    }

    @Test
    void shouldRemainImmutableWhenCopied() {
        UUID tenantId = UUID.randomUUID();
        TenantContext original = new TenantContext(tenantId);
        TenantContext copy = original.withTenantId(tenantId);

        assertEquals(tenantId, original.tenantId());
        assertEquals(tenantId, copy.tenantId());
        assertNotSame(original, copy);
    }
}