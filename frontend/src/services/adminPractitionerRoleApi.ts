export interface AdminPractitionerRole {
  id: string;
  tenantId: string;
  organizationId: string;
  locationId: string;
  practitionerId: string;
  roleCode: string;
  active: boolean;
  primaryRole: boolean;
  periodStart: string | null;
  periodEnd: string | null;
  fhirCodeJson: string | null;
  fhirSpecialtyJson: string | null;
  fhirTelecomJson: string | null;
  fhirAvailableTimeJson: string | null;
}

export interface CreateAdminPractitionerRoleRequest {
  organizationId: string;
  locationId: string;
  practitionerId: string;
  roleCode: string;
  primaryRole?: boolean;
  periodStart?: string;
  periodEnd?: string;
  fhirCodeJson?: string;
  fhirSpecialtyJson?: string;
  fhirTelecomJson?: string;
  fhirAvailableTimeJson?: string;
}

export interface UpdateAdminPractitionerRoleRequest {
  roleCode?: string;
  active?: boolean;
  primaryRole?: boolean;
  periodStart?: string;
  periodEnd?: string;
  fhirCodeJson?: string;
  fhirSpecialtyJson?: string;
  fhirTelecomJson?: string;
  fhirAvailableTimeJson?: string;
}

function tenantHeaders(tenantId: string, includeJson = false, sessionId?: string): HeadersInit {
  const headers: Record<string, string> = { 'X-Tenant-ID': tenantId };
  if (includeJson) headers['Content-Type'] = 'application/json';
  if (sessionId) headers['Authorization'] = `Bearer ${sessionId}`;
  return headers;
}

export async function listAdminPractitionerRoles(tenantId: string, sessionId?: string): Promise<AdminPractitionerRole[]> {
  const response = await fetch('/api/admin/practitioner-roles', {
    credentials: 'include',
    headers: tenantHeaders(tenantId, false, sessionId),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const err = Object.assign(new Error(`listAdminPractitionerRoles failed: ${response.status}`), {
      status: response.status,
      body,
    });
    throw err;
  }

  return response.json() as Promise<AdminPractitionerRole[]>;
}

export async function createAdminPractitionerRole(
  tenantId: string,
  payload: CreateAdminPractitionerRoleRequest,
): Promise<AdminPractitionerRole> {
  const response = await fetch('/api/admin/practitioner-roles', {
    method: 'POST',
    credentials: 'include',
    headers: tenantHeaders(tenantId, true),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw Object.assign(new Error(`createAdminPractitionerRole failed: ${response.status}`), { status: response.status, body });
  }
  return response.json() as Promise<AdminPractitionerRole>;
}

export async function updateAdminPractitionerRole(
  tenantId: string,
  roleId: string,
  payload: UpdateAdminPractitionerRoleRequest,
): Promise<AdminPractitionerRole> {
  const response = await fetch(`/api/admin/practitioner-roles/${roleId}`, {
    method: 'PUT',
    credentials: 'include',
    headers: tenantHeaders(tenantId, true),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw Object.assign(new Error(`updateAdminPractitionerRole failed: ${response.status}`), { status: response.status, body });
  }
  return response.json() as Promise<AdminPractitionerRole>;
}

export async function deleteAdminPractitionerRole(tenantId: string, roleId: string): Promise<void> {
  const response = await fetch(`/api/admin/practitioner-roles/${roleId}/deactivate`, {
    method: 'POST',
    credentials: 'include',
    headers: tenantHeaders(tenantId),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw Object.assign(new Error(`deleteAdminPractitionerRole failed: ${response.status}`), { status: response.status, body });
  }
}
