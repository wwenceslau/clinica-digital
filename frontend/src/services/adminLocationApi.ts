export interface AdminLocation {
  id: string;
  tenantId: string;
  organizationId: string;
  displayName: string;
  fhirName: string;
  fhirStatus: string;
  fhirMode: string;
  accountActive: boolean;
  fhirTelecomJson: string | null;
  fhirAddressJson: string | null;
}

export interface CreateAdminLocationRequest {
  organizationId: string;
  displayName: string;
  fhirName?: string;
  fhirStatus?: string;
  fhirMode?: string;
  fhirTelecomJson?: string;
  fhirAddressJson?: string;
}

export interface UpdateAdminLocationRequest {
  displayName?: string;
  fhirName?: string;
  fhirStatus?: string;
  fhirMode?: string;
  accountActive?: boolean;
  fhirTelecomJson?: string;
  fhirAddressJson?: string;
}

function tenantHeaders(tenantId: string, includeJson = false, sessionId?: string): HeadersInit {
  const headers: Record<string, string> = { 'X-Tenant-ID': tenantId };
  if (includeJson) headers['Content-Type'] = 'application/json';
  if (sessionId) headers['Authorization'] = `Bearer ${sessionId}`;
  return headers;
}

export async function listAdminLocations(tenantId: string, sessionId?: string): Promise<AdminLocation[]> {
  const response = await fetch('/api/admin/locations', {
    credentials: 'include',
    headers: tenantHeaders(tenantId, false, sessionId),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`listAdminLocations failed: ${response.status}`), {
      status: response.status,
      body,
    });
    throw err;
  }

  return response.json() as Promise<AdminLocation[]>;
}

export async function createAdminLocation(tenantId: string, payload: CreateAdminLocationRequest): Promise<AdminLocation> {
  const response = await fetch('/api/admin/locations', {
    method: 'POST',
    credentials: 'include',
    headers: tenantHeaders(tenantId, true),
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`createAdminLocation failed: ${response.status}`), {
      status: response.status,
      body,
    });
    throw err;
  }

  return response.json() as Promise<AdminLocation>;
}

export async function updateAdminLocation(
  tenantId: string,
  locationId: string,
  payload: UpdateAdminLocationRequest,
): Promise<AdminLocation> {
  const response = await fetch(`/api/admin/locations/${locationId}`, {
    method: 'PUT',
    credentials: 'include',
    headers: tenantHeaders(tenantId, true),
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`updateAdminLocation failed: ${response.status}`), {
      status: response.status,
      body,
    });
    throw err;
  }

  return response.json() as Promise<AdminLocation>;
}

export async function deactivateAdminLocation(tenantId: string, locationId: string): Promise<AdminLocation> {
  const response = await fetch(`/api/admin/locations/${locationId}/deactivate`, {
    method: 'POST',
    credentials: 'include',
    headers: tenantHeaders(tenantId),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`deactivateAdminLocation failed: ${response.status}`), {
      status: response.status,
      body,
    });
    throw err;
  }

  return response.json() as Promise<AdminLocation>;
}
