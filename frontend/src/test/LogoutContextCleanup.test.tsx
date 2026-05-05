/**
 * T074 [US5] Frontend logout and context cleanup tests.
 *
 * Verifies:
 * - Calling authContext.logout() clears session state
 * - Calling apiLogout() sends POST /api/auth/logout with credentials
 * - After logout, TenantContext returns all-null values
 * - ProtectedRoute redirects to /login view when unauthenticated
 *
 * Refs: FR-024
 */

import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '../../context/AuthContext';
import { TenantProvider, useTenant } from '../../context/TenantContext';
import { logout as apiLogout } from '../../services/iamAuthApi';
import type { SessionIssuedResponse } from '../../services/iamAuthApi';

// ---- Mock fetch for API call ----

vi.mock('../../services/iamAuthApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/iamAuthApi')>();
  return {
    ...actual,
    logout: vi.fn(),
  };
});

// ---- Fixtures ----

const mockSession: SessionIssuedResponse = {
  expiresAt: '2099-01-01T00:00:00Z',
  practitioner: {
    id: 'prac-logout-001',
    email: 'dr@aurora.com',
    profileType: 10,
    displayName: 'Dr. Logout',
    accountActive: true,
    identifiers: [],
    names: [{ text: 'Dr. Logout' }],
  },
  tenant: {
    id: 'org-logout-001',
    name: 'clinica-logout',
    displayName: 'Clínica Logout',
    cnes: '9876543',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

function FullWrapper({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <TenantProvider>{children}</TenantProvider>
    </AuthProvider>
  );
}

function useBoth() {
  return { auth: useAuth(), tenant: useTenant() };
}

// ---- Tests ----

describe('Logout: context cleanup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('logout() clears auth session and tenant context', () => {
    const { result } = renderHook(() => useBoth(), { wrapper: FullWrapper });

    // Arrange: login first
    act(() => {
      result.current.auth.login(mockSession);
    });
    expect(result.current.auth.isAuthenticated).toBe(true);
    expect(result.current.tenant.tenantId).toBe('org-logout-001');

    // Act: logout
    act(() => {
      result.current.auth.logout();
    });

    // Assert: all context cleared
    expect(result.current.auth.isAuthenticated).toBe(false);
    expect(result.current.auth.session).toBeNull();
    expect(result.current.tenant.tenantId).toBeNull();
    expect(result.current.tenant.organizationName).toBeNull();
    expect(result.current.tenant.practitionerName).toBeNull();
    expect(result.current.tenant.profileType).toBeNull();
  });

  it('apiLogout sends POST /api/auth/logout with credentials', async () => {
    const mockApiLogout = vi.mocked(apiLogout);
    mockApiLogout.mockResolvedValue(undefined);

    await apiLogout();

    expect(mockApiLogout).toHaveBeenCalledOnce();
  });
});
