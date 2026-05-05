/**
 * T042 [P] [US3] Frontend unit test for ClinicRegistrationForm component.
 *
 * Verifies:
 * 1. Form renders required fields: displayName, cnes, email, cpf, password.
 * 2. Submitting valid data invokes onSuccess callback.
 * 3. On 409 conflict response, CNES conflict alert is visible.
 * 4. On 400 validation error, validation error alert is visible.
 *
 * Refs: FR-003, FR-009, FR-013
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { ClinicRegistrationForm } from '../../components/organisms/ClinicRegistrationForm';

// Mock the API service to control responses
vi.mock('../../services/clinicRegistrationApi', () => ({
  registerClinic: vi.fn(),
}));

import { registerClinic } from '../../services/clinicRegistrationApi';
const mockedRegisterClinic = vi.mocked(registerClinic);

describe('ClinicRegistrationForm', () => {
  const onSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all required form fields', () => {
    render(<ClinicRegistrationForm onSuccess={onSuccess} />);

    expect(screen.getByLabelText(/nome da clínica/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/cnes/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/e-?mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/cpf/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/senha/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /registrar/i })).toBeInTheDocument();
  });

  it('calls onSuccess after successful registration', async () => {
    mockedRegisterClinic.mockResolvedValueOnce({
      tenantId: 'uuid-tenant',
      adminPractitionerId: 'uuid-admin',
      organization: { displayName: 'Clinica Test', cnes: '1234567', accountActive: true },
      adminPractitioner: {
        id: 'uuid-admin',
        email: 'admin@test.local',
        profileType: 10,
        displayName: 'Admin Test',
        accountActive: true,
      },
    });

    render(<ClinicRegistrationForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText(/nome da clínica/i), 'Clinica Test');
    await userEvent.type(screen.getByLabelText(/cnes/i), '1234567');
    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'admin@test.local');
    await userEvent.type(screen.getByLabelText(/cpf/i), '12345678901');
    await userEvent.type(screen.getByLabelText(/senha/i), 'S3nha!Forte');

    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledTimes(1);
    });
  });

  it('shows CNES conflict alert on 409 response', async () => {
    mockedRegisterClinic.mockRejectedValueOnce({
      status: 409,
      body: {
        resourceType: 'OperationOutcome',
        issue: [{ severity: 'error', code: 'conflict', diagnostics: 'CNES já cadastrado' }],
      },
    });

    render(<ClinicRegistrationForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText(/nome da clínica/i), 'Clinica Duplicada');
    await userEvent.type(screen.getByLabelText(/cnes/i), '1234567');
    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'admin@dup.local');
    await userEvent.type(screen.getByLabelText(/cpf/i), '98765432100');
    await userEvent.type(screen.getByLabelText(/senha/i), 'S3nha!Forte');

    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByRole('alert')).toHaveTextContent(/cnes/i);
    });

    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('shows validation error alert on 400 response', async () => {
    mockedRegisterClinic.mockRejectedValueOnce({
      status: 400,
      body: {
        resourceType: 'OperationOutcome',
        issue: [{ severity: 'error', code: 'invalid', diagnostics: 'cnes must be exactly 7 numeric digits' }],
      },
    });

    render(<ClinicRegistrationForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText(/nome da clínica/i), 'Clinica Invalida');
    await userEvent.type(screen.getByLabelText(/cnes/i), 'ABCDE');
    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'admin@inv.local');
    await userEvent.type(screen.getByLabelText(/cpf/i), '12345678901');
    await userEvent.type(screen.getByLabelText(/senha/i), 'S3nha!Forte');

    fireEvent.click(screen.getByRole('button', { name: /registrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    expect(onSuccess).not.toHaveBeenCalled();
  });
});
