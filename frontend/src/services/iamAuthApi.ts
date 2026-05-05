/**
 * T058 [US4] API service for IAM authentication flows.
 *
 * Thin fetch wrappers for POST /api/auth/login and
 * POST /api/auth/select-organization.
 * On non-2xx responses, parses the FHIR OperationOutcome body and
 * rejects with a structured error object containing status and body.
 *
 * Refs: FR-004, FR-013
 */

import type { OperationOutcome } from './clinicRegistrationApi';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface OrgOption {
  organizationId: string;
  displayName: string;
  cnes: string;
}

export interface PractitionerSummary {
  id: string;
  email: string;
  profileType: number;
  displayName: string;
  accountActive: boolean;
  identifiers: { system: string; value: string }[];
  names: { text: string }[];
}

export interface OrganizationSummary {
  id: string;
  name: string;
  displayName: string;
  cnes: string;
  active: boolean;
  accountActive: boolean;
  identifiers: { system: string; value: string }[];
}

export interface SessionIssuedResponse {
  sessionId: string;
  expiresAt: string;
  practitioner: PractitionerSummary;
  tenant: OrganizationSummary;
}

export interface LoginSingleOrgResponse {
  mode: 'single';
  session: SessionIssuedResponse;
}

export interface LoginMultiOrgResponse {
  mode: 'multiple';
  challengeToken: string;
  organizations: OrgOption[];
}

export type LoginResponse = LoginSingleOrgResponse | LoginMultiOrgResponse;

export interface IamAuthError {
  status: number;
  body: OperationOutcome | null;
}

async function parseOutcome(response: Response): Promise<OperationOutcome | null> {
  try {
    return (await response.json()) as OperationOutcome;
  } catch {
    return null;
  }
}

/**
 * Raw shape returned by the backend MultiOrgAuthController (flat record).
 * Not exported — internal mapping only.
 */
interface RawLoginResponse {
  mode: string;
  sessionId?: string;
  expiresAt?: string;
  /** Single-org: resolved organization UUID. */
  organizationId?: string;
  userId?: string;
  challengeToken?: string;
  organizations?: Array<{ organizationId: string; displayName: string; cnes: string }>;
  traceId?: string;
}

/**
 * Builds a SessionIssuedResponse from the fields available in the backend's
 * flat login response. practitioner.profileType is set to -1 as a placeholder;
 * the real value is resolved asynchronously by TenantContext via GET
 * /api/users/me/context and consumed by RbacPermissionGuard via TenantContext.
 */
const SYSTEM_TENANT_ID = '00000000-0000-0000-0000-000000000000';

function buildSession(raw: { sessionId: string; expiresAt: string; practitionerId: string; tenantId: string; isSuperUser?: boolean; email?: string }): SessionIssuedResponse {
  const email = raw.email ?? '';
  const displayName = email ? email.split('@')[0] : '';
  return {
    sessionId: raw.sessionId,
    expiresAt: raw.expiresAt,
    practitioner: {
      id: raw.practitionerId,
      email,
      profileType: raw.isSuperUser ? 0 : -1,
      displayName,
      accountActive: true,
      identifiers: [],
      names: [],
    },
    tenant: {
      id: raw.tenantId,
      name: '',
      displayName: raw.isSuperUser ? 'Super Admin' : '',
      cnes: '',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  };
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (response.ok) {
    const raw = (await response.json()) as RawLoginResponse;
    if (raw.mode === 'single') {
      const isSuperUser = raw.organizationId == null;
      return {
        mode: 'single',
        session: buildSession({
          sessionId: String(raw.sessionId!),
          expiresAt: String(raw.expiresAt!),
          practitionerId: String(raw.userId!),
          tenantId: isSuperUser ? SYSTEM_TENANT_ID : String(raw.organizationId!),
          isSuperUser,
          email: request.email,
        }),
      };
    }
    // multi-org challenge
    return {
      mode: 'multiple',
      challengeToken: String(raw.challengeToken!),
      organizations: (raw.organizations ?? []).map((o) => ({
        organizationId: String(o.organizationId),
        displayName: o.displayName,
        cnes: o.cnes,
      })),
    };
  }

  const body = await parseOutcome(response);
  return Promise.reject({ status: response.status, body } satisfies IamAuthError);
}

export async function selectOrganization(
  challengeToken: string,
  organizationId: string,
): Promise<SessionIssuedResponse> {
  const response = await fetch('/api/auth/select-organization', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ challengeToken, organizationId }),
  });

  if (response.ok) {
    const raw = (await response.json()) as {
      sessionId: string;
      expiresAt: string;
      tenantId?: string;
      organizationId?: string;
      userId: string;
    };
    return buildSession({
      sessionId: String(raw.sessionId),
      expiresAt: String(raw.expiresAt),
      practitionerId: String(raw.userId),
      tenantId: String(raw.tenantId ?? raw.organizationId ?? ''),
    });
  }

  const body = await parseOutcome(response);
  return Promise.reject({ status: response.status, body } satisfies IamAuthError);
}

/**
 * T080 [US5] POST /api/auth/logout — invalidates the server-side session and
 * clears the session cookie.  Must be called before clearing client-side state.
 *
 * Refs: FR-024
 */
export async function logout(): Promise<void> {
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
  });

  if (!response.ok) {
    const body = await parseOutcome(response);
    return Promise.reject({ status: response.status, body } satisfies IamAuthError);
  }
}

/**
 * T088 [US11] The full resolved user context returned by GET /api/users/me/context
 * and POST /api/users/me/active-location.
 *
 * Refs: FR-008, FR-019, US11
 */
export interface UserContextResponse {
  tenantId: string;
  organizationId: string;
  organizationName: string;
  locationId: string | null;
  locationName: string | null;
  practitionerId: string | null;
  practitionerName: string | null;
  profileType: number;
  practitionerRoleId: string | null;
  roleCode: string | null;
}

/**
 * T088 [US11] GET /api/users/me/context — returns the resolved context for the
 * authenticated session (org, location, practitioner, role).
 *
 * Refs: FR-008, FR-019
 */
export async function getMyContext(tenantId?: string): Promise<UserContextResponse> {
  const headers: Record<string, string> = {};
  if (tenantId) {
    headers['X-Tenant-ID'] = tenantId;
  }
  const response = await fetch('/api/users/me/context', {
    method: 'GET',
    credentials: 'include',
    headers,
  });

  if (response.ok) {
    return response.json() as Promise<UserContextResponse>;
  }

  const body = await parseOutcome(response);
  return Promise.reject({ status: response.status, body } satisfies IamAuthError);
}

// ── Profile-20 user management (T097, T098) ──────────────────────────────────

export interface CreateProfile20UserRequest {
  practitioner: {
    displayName: string;
    email: string;
    cpf: string;
    password: string;
  };
  locationId: string;
  roleCode: string;
}

export interface CreateProfile20UserResponse {
  userId: string;
  practitionerId: string;
  practitionerRoleId: string;
  practitioner: PractitionerSummary;
}

/**
 * T097 POST /api/admin/users — creates a new profile-20 user within the
 * caller's tenant.  Requires admin session (profile=10).
 *
 * Refs: FR-006, FR-009, FR-011
 */
export async function createProfile20User(
  request: CreateProfile20UserRequest,
): Promise<CreateProfile20UserResponse> {
  const response = await fetch('/api/admin/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  if (response.ok) {
    return response.json() as Promise<CreateProfile20UserResponse>;
  }

  const body = await parseOutcome(response);
  return Promise.reject({ status: response.status, body } satisfies IamAuthError);
}

/**
 * T088 [US11] POST /api/users/me/active-location — selects the active location
 * for the session.  Returns the updated context on success.
 *
 * Refs: FR-018
 */
export async function setActiveLocation(locationId: string): Promise<UserContextResponse> {
  const response = await fetch('/api/users/me/active-location', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ locationId }),
  });

  if (response.ok) {
    return response.json() as Promise<UserContextResponse>;
  }

  const body = await parseOutcome(response);
  return Promise.reject({ status: response.status, body } satisfies IamAuthError);
}

