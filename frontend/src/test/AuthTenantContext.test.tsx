/**
 * T071 [US5] Unit tests for AuthContext and TenantContext.
 *
 * Verifies:
 * - AuthProvider initial state is unauthenticated
 * - login() stores session and marks isAuthenticated
 * - logout() clears session and resets isAuthenticated
 * - TenantProvider derives correct fields from session
 * - TenantProvider returns nulls when unauthenticated
 * - Hooks throw when used outside their providers
 *
 * Refs: FR-012
 */

import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from '../../context/AuthContext';
import { TenantProvider, useTenant } from '../../context/TenantContext';
import type { SessionIssuedResponse } from '../../services/iamAuthApi';

// ---- Fixtures ----

const mockSession: SessionIssuedResponse = {
  expiresAt: '2099-01-01T00:00:00Z',
  practitioner: {
    id: 'prac-001',
    email: 'dr.silva@aurora.com',
    profileType: 10,
    displayName: 'Dr. Silva',
    accountActive: true,
    identifiers: [],
    names: [{ text: 'Dr. Silva' }],
  },
  tenant: {
    id: 'org-001',
    name: 'clinica-aurora',
    displayName: 'Clínica Aurora',
    cnes: '1234567',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

// ---- Wrappers ----

function AuthWrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}

function FullWrapper({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <TenantProvider>{children}</TenantProvider>
    </AuthProvider>
  );
}

// ---- AuthContext tests ----

describe('AuthContext', () => {
  it('starts unauthenticated with null session', () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthWrapper });
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.session).toBeNull();
  });

  it('login() sets session and isAuthenticated', () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthWrapper });

    act(() => {
      result.current.login(mockSession);
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.session).toEqual(mockSession);
  });

  it('logout() clears session and resets isAuthenticated', () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthWrapper });

    act(() => {
      result.current.login(mockSession);
    });
    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.session).toBeNull();
  });

  it('useAuth throws when used outside AuthProvider', () => {
    expect(() =>
      renderHook(() => useAuth()),
    ).toThrow('useAuth must be used inside AuthProvider');
  });
});

// ---- TenantContext tests ----

describe('TenantContext', () => {
  it('returns nulls when not authenticated', () => {
    const { result } = renderHook(() => useTenant(), { wrapper: FullWrapper });

    expect(result.current.tenantId).toBeNull();
    expect(result.current.organizationId).toBeNull();
    expect(result.current.organizationName).toBeNull();
    expect(result.current.practitionerId).toBeNull();
    expect(result.current.practitionerName).toBeNull();
    expect(result.current.profileType).toBeNull();
  });

  it('derives context fields from session after login', () => {
    // Need combined hook to call both auth and tenant
    function useBoth() {
      return { auth: useAuth(), tenant: useTenant() };
    }

    const { result } = renderHook(() => useBoth(), { wrapper: FullWrapper });

    act(() => {
      result.current.auth.login(mockSession);
    });

    const tenant = result.current.tenant;
    expect(tenant.tenantId).toBe('org-001');
    expect(tenant.organizationId).toBe('org-001');
    expect(tenant.organizationName).toBe('Clínica Aurora');
    expect(tenant.practitionerId).toBe('prac-001');
    expect(tenant.practitionerName).toBe('Dr. Silva');
    expect(tenant.profileType).toBe(10);
  });

  it('clears context fields after logout', () => {
    function useBoth() {
      return { auth: useAuth(), tenant: useTenant() };
    }

    const { result } = renderHook(() => useBoth(), { wrapper: FullWrapper });

    act(() => {
      result.current.auth.login(mockSession);
    });
    expect(result.current.tenant.tenantId).toBe('org-001');

    act(() => {
      result.current.auth.logout();
    });

    expect(result.current.tenant.tenantId).toBeNull();
    expect(result.current.tenant.practitionerName).toBeNull();
  });

  it('useTenant throws when used outside TenantProvider', () => {
    expect(() =>
      renderHook(() => useTenant(), { wrapper: AuthWrapper }),
    ).toThrow('useTenant must be used inside TenantProvider');
  });
});
