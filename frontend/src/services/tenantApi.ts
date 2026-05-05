/**
 * T136 [US2] Tenant API service — wraps authenticated calls to AdminTenantController.
 *
 * All requests use session cookies (credentials: 'include').
 * No sentinel X-Tenant-ID header — authentication is via opaque session cookie
 * issued by /api/auth/login. Endpoints are under /api/admin/tenants which is
 * covered by AuthenticationFilter.
 *
 * Refs: FR-003, FR-009, FR-022
 */

export interface TenantDto {
  id: string;
  slug: string;
  legalName: string;
  status: string;
  planTier: string;
  adminDisplayName?: string;
  adminEmail?: string;
  adminCpf?: string;
}

export interface CreateTenantRequest {
  organization: {
    displayName: string;
    cnes: string;
  };
  adminPractitioner: {
    displayName: string;
    email: string;
    cpf: string;
    password: string;
  };
}

export interface CreateTenantResponse {
  tenantId: string;
  adminPractitionerId: string;
  organization: {
    displayName: string;
    cnes: string;
    accountActive: boolean;
  };
  adminPractitioner: {
    id: string;
    email: string;
    profileType: number;
    displayName: string;
    accountActive: boolean;
  };
}

export interface UpdateTenantRequest {
  organization: {
    displayName: string;
    cnes: string;
  };
  adminPractitioner: {
    displayName: string;
    email: string;
    cpf: string;
    password: string;
  };
  planTier?: string;
}

export async function listTenants(): Promise<TenantDto[]> {
  const response = await fetch('/api/admin/tenants', {
    credentials: 'include',
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`listTenants failed: ${response.status}`), { status: response.status, body });
    throw err;
  }
  const data = await response.json() as Array<{
    id: string;
    slug: string;
    legalName: string;
    status: string;
    planTier: string;
    adminDisplayName?: string;
    adminEmail?: string;
    adminCpf?: string;
  }>;
  return data;
}

export async function createTenantApi(
  payload: CreateTenantRequest,
): Promise<CreateTenantResponse> {
  const response = await fetch('/api/admin/tenants', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`createTenant failed: ${response.status}`), { status: response.status, body });
    throw err;
  }
  return response.json() as Promise<CreateTenantResponse>;
}

export async function updateTenantApi(
  tenantId: string,
  payload: UpdateTenantRequest,
): Promise<TenantDto> {
  const response = await fetch(`/api/admin/tenants/${tenantId}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`updateTenant failed: ${response.status}`), { status: response.status, body });
    throw err;
  }
  return response.json() as Promise<TenantDto>;
}

export async function deleteTenantApi(tenantId: string): Promise<void> {
  const response = await fetch(`/api/admin/tenants/${tenantId}`, {
    method: 'DELETE',
    credentials: 'include',
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`deleteTenant failed: ${response.status}`), { status: response.status, body });
    throw err;
  }
}

