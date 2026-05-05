/**
 * T047 [US3] API service for public clinic registration.
 *
 * Thin fetch wrapper for POST /api/public/clinic-registration.
 * On non-2xx responses, parses the FHIR OperationOutcome body and
 * rejects with a structured error object containing status and body.
 *
 * Refs: FR-003, FR-009
 */

export interface OperationOutcomeIssue {
  severity: string;
  code: string;
  diagnostics?: string;
}

export interface OperationOutcome {
  resourceType: 'OperationOutcome';
  issue: OperationOutcomeIssue[];
}

export interface ClinicRegistrationError {
  status: number;
  body: OperationOutcome | null;
}

export interface ClinicRegistrationRequest {
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

export interface ClinicRegistrationResponse {
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

export async function registerClinic(
  request: ClinicRegistrationRequest,
): Promise<ClinicRegistrationResponse> {
  const response = await fetch('/api/public/clinic-registration', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (response.ok) {
    return response.json() as Promise<ClinicRegistrationResponse>;
  }

  let body: OperationOutcome | null = null;
  try {
    body = (await response.json()) as OperationOutcome;
  } catch {
    // non-JSON error body — leave body as null
  }

  const error: ClinicRegistrationError = { status: response.status, body };
  return Promise.reject(error);
}
