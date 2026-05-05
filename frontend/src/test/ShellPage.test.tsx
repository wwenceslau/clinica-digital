/**
 * T142 [P] [US5] Unit test: ShellPage injects real auth context into MainTemplate.trainingContext.
 *
 * Verifies:
 * 1. user_id corresponds to session.practitioner.id
 * 2. role corresponds to session.practitioner.profileType (as string in array)
 * 3. tenant_id corresponds to session.tenant.id (via TenantContext)
 * 4. ShellPage renders a loading fallback when session is null (not yet loaded)
 *
 * TDD state: RED until T137 is implemented (ShellPage currently hardcodes all values).
 *
 * Refs: FR-007, FR-012
 */

import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import React from 'react';
import type { SessionIssuedResponse } from '../../services/iamAuthApi';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockLogin = vi.fn();
const mockLogout = vi.fn();

let mockIsAuthenticated = false;
let mockSession: SessionIssuedResponse | null = null;

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: mockIsAuthenticated,
    session: mockSession,
    login: mockLogin,
    logout: mockLogout,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

let mockTenantId: string | null = null;
let mockPractitionerId: string | null = null;
let mockProfileType: number | null = null;

vi.mock('../../context/TenantContext', () => ({
  useTenant: () => ({
    tenantId: mockTenantId,
    organizationId: mockTenantId,
    organizationName: null,
    locationId: null,
    locationName: null,
    practitionerId: mockPractitionerId,
    practitionerName: null,
    profileType: mockProfileType,
    setActiveLocation: vi.fn(),
  }),
  TenantProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Capture trainingContext passed to MainTemplate
let capturedTrainingContext: Record<string, unknown> | null = null;

vi.mock('../../components/templates/MainTemplate', () => ({
  MainTemplate: ({ trainingContext, children }: { trainingContext: Record<string, unknown>; children: React.ReactNode }) => {
    capturedTrainingContext = trainingContext;
    return (
      <div data-testid="main-template" data-training-context={JSON.stringify(trainingContext)}>
        {children}
      </div>
    );
  },
}));

// ---------------------------------------------------------------------------
// Import after mocks
// ---------------------------------------------------------------------------

// ShellPage will be exported from App.tsx once T137 is implemented.
// Until then this import is the TDD RED marker — test expects named export.
import { ShellPage } from '../../app/App';

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const SESSION_PROFILE0: SessionIssuedResponse = {
  sessionId: 'session-001',
  expiresAt: '2099-12-31T23:59:59Z',
  practitioner: {
    id: 'prac-super-uuid',
    email: 'super@clinicadigital.local',
    profileType: 0,
    displayName: 'Super Admin',
    accountActive: true,
    identifiers: [],
    names: [{ text: 'Super Admin' }],
  },
  tenant: {
    id: 'tenant-uuid-001',
    name: 'clinicadigital',
    displayName: 'Clínica Digital',
    cnes: '0000000',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

function renderShellPage(child = <div data-testid="child">content</div>) {
  capturedTrainingContext = null;
  return render(
    <MemoryRouter>
      <ShellPage>{child}</ShellPage>
    </MemoryRouter>
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('ShellPage — trainingContext from real auth', () => {
  beforeEach(() => {
    capturedTrainingContext = null;
  });

  it('renders loading fallback when session is null', () => {
    mockIsAuthenticated = false;
    mockSession = null;
    mockTenantId = null;
    mockPractitionerId = null;
    mockProfileType = null;

    renderShellPage();

    // Should NOT render main-template when session is null
    expect(screen.queryByTestId('main-template')).toBeNull();
  });

  it('maps user_id to session.practitioner.id', () => {
    mockIsAuthenticated = true;
    mockSession = SESSION_PROFILE0;
    mockTenantId = SESSION_PROFILE0.tenant.id;
    mockPractitionerId = SESSION_PROFILE0.practitioner.id;
    mockProfileType = SESSION_PROFILE0.practitioner.profileType;

    renderShellPage();

    expect(capturedTrainingContext).not.toBeNull();
    expect(capturedTrainingContext?.user_id).toBe('prac-super-uuid');
  });

  it('maps tenant_id to tenant context tenantId', () => {
    mockIsAuthenticated = true;
    mockSession = SESSION_PROFILE0;
    mockTenantId = 'tenant-uuid-001';
    mockPractitionerId = SESSION_PROFILE0.practitioner.id;
    mockProfileType = SESSION_PROFILE0.practitioner.profileType;

    renderShellPage();

    expect(capturedTrainingContext?.tenant_id).toBe('tenant-uuid-001');
  });

  it('maps role from session.practitioner.profileType', () => {
    mockIsAuthenticated = true;
    mockSession = SESSION_PROFILE0;
    mockTenantId = SESSION_PROFILE0.tenant.id;
    mockPractitionerId = SESSION_PROFILE0.practitioner.id;
    mockProfileType = 0;

    renderShellPage();

    // role should be an array containing the profileType value
    expect(Array.isArray(capturedTrainingContext?.role)).toBe(true);
    const role = capturedTrainingContext?.role as string[];
    expect(role).toContain('0');
  });

  it('does not use hardcoded values after login', async () => {
    mockIsAuthenticated = true;
    mockSession = SESSION_PROFILE0;
    mockTenantId = SESSION_PROFILE0.tenant.id;
    mockPractitionerId = SESSION_PROFILE0.practitioner.id;
    mockProfileType = SESSION_PROFILE0.practitioner.profileType;

    renderShellPage();

    expect(capturedTrainingContext?.tenant_id).not.toBe('tenant-clinica-digital');
    expect(capturedTrainingContext?.user_id).not.toBe('admin@clinica.local');
    expect(capturedTrainingContext?.role).not.toEqual(['support']);
  });
});
