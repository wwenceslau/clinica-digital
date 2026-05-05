/**
 * T104 [US6] API service for RBAC group management.
 *
 * Thin fetch wrappers for AdminGroupController endpoints.
 * On non-2xx responses, parses the FHIR OperationOutcome body and rejects
 * with a structured error object containing status and body.
 *
 * Refs: FR-006
 */

export interface IamGroup {
  groupId: string;
  tenantId: string;
  name: string;
  description: string;
}

export interface IamPermission {
  permissionId: string;
  code: string;
  resource: string;
  action: string;
  description: string | null;
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.ok) {
    return res.json() as Promise<T>;
  }
  const body = await res.text();
  throw { status: res.status, body };
}

function authHeaders(tenantId: string, sessionId: string): HeadersInit {
  return {
    "Content-Type": "application/json",
    "X-Tenant-ID": tenantId,
    Authorization: `Bearer ${sessionId}`,
  };
}

export async function createGroup(
  tenantId: string,
  sessionId: string,
  name: string,
  description: string,
): Promise<IamGroup> {
  const res = await fetch("/api/admin/groups", {
    method: "POST",
    headers: authHeaders(tenantId, sessionId),
    body: JSON.stringify({ name, description }),
  });
  return handleResponse<IamGroup>(res);
}

export async function listGroups(tenantId: string, sessionId: string): Promise<IamGroup[]> {
  const res = await fetch("/api/admin/groups", {
    headers: authHeaders(tenantId, sessionId),
  });
  return handleResponse<IamGroup[]>(res);
}

export async function listPermissions(
  tenantId: string,
  sessionId: string,
): Promise<IamPermission[]> {
  const res = await fetch("/api/admin/permissions", {
    headers: authHeaders(tenantId, sessionId),
  });
  return handleResponse<IamPermission[]>(res);
}

export async function assignUserToGroup(
  tenantId: string,
  sessionId: string,
  groupId: string,
  userId: string,
): Promise<void> {
  const res = await fetch(`/api/admin/groups/${groupId}/members`, {
    method: "POST",
    headers: authHeaders(tenantId, sessionId),
    body: JSON.stringify({ userId }),
  });
  if (!res.ok) {
    const body = await res.text();
    throw { status: res.status, body };
  }
}

export async function assignPermissionToGroup(
  tenantId: string,
  sessionId: string,
  groupId: string,
  permissionId: string,
): Promise<void> {
  const res = await fetch(`/api/admin/groups/${groupId}/permissions`, {
    method: "POST",
    headers: authHeaders(tenantId, sessionId),
    body: JSON.stringify({ permissionId }),
  });
  if (!res.ok) {
    const body = await res.text();
    throw { status: res.status, body };
  }
}

export async function listGroupPermissions(
  tenantId: string,
  sessionId: string,
  groupId: string,
): Promise<IamPermission[]> {
  const res = await fetch(`/api/admin/groups/${groupId}/permissions`, {
    headers: authHeaders(tenantId, sessionId),
  });
  return handleResponse<IamPermission[]>(res);
}

export async function listUserPermissions(
  tenantId: string,
  sessionId: string,
  userId: string,
): Promise<IamPermission[]> {
  const res = await fetch(`/api/admin/users/${userId}/permissions`, {
    headers: authHeaders(tenantId, sessionId),
  });
  return handleResponse<IamPermission[]>(res);
}
