/**
 * T113 [US10] Unit tests for LoginForm organism.
 *
 * Verifies:
 * 1. Renders email and password fields with correct labels and ARIA attributes.
 * 2. Renders submit button ("Entrar").
 * 3. On single-org success: calls onLogin with the session payload.
 * 4. On multi-org response: renders org-selection form (data-testid="organization-select-form").
 * 5. On API error: displays OperationOutcome alert.
 * 6. Wrapped in AuthTemplate, renders within auth-template container.
 *
 * Refs: FR-013, US10
 */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { LoginForm } from '../../components/organisms/LoginForm';
import { AuthTemplate } from '../../components/templates/AuthTemplate';

vi.mock('../../services/iamAuthApi', () => ({
  login: vi.fn(),
  selectOrganization: vi.fn(),
}));

import { login as mockLogin, selectOrganization as mockSelectOrg } from '../../services/iamAuthApi';
const mockedLogin = vi.mocked(mockLogin as (...args: unknown[]) => Promise<unknown>);
const mockedSelectOrg = vi.mocked(mockSelectOrg as (...args: unknown[]) => Promise<unknown>);

const SINGLE_ORG_SESSION = {
  mode: 'single' as const,
  session: {
    expiresAt: '2099-01-01T00:00:00Z',
    practitioner: {
      id: 'prac-001',
      email: 'dr@aurora.com',
      profileType: 20,
      displayName: 'Dr. Aurora',
      accountActive: true,
      identifiers: [],
      names: [{ text: 'Dr. Aurora' }],
    },
    tenant: {
      id: 'tenant-001',
      name: 'clinica-aurora',
      displayName: 'Clínica Aurora',
      cnes: '1234567',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  },
};

const MULTI_ORG_RESPONSE = {
  mode: 'multiple' as const,
  challengeToken: 'tok-abc',
  organizations: [
    { organizationId: 'uuid-org-1', displayName: 'Clínica Aurora', cnes: '1234567' },
    { organizationId: 'uuid-org-2', displayName: 'Clínica Horizonte', cnes: '7654321' },
  ],
};

const OPERATION_OUTCOME_ERROR = {
  resourceType: 'OperationOutcome',
  issue: [{ severity: 'error', code: 'security', diagnostics: 'Credenciais inválidas.' }],
};

describe('LoginForm', () => {
  const onLogin = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ----- Rendering -----

  it('renders email and password fields', () => {
    render(<LoginForm onLogin={onLogin} />);
    expect(screen.getByLabelText(/e-?mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/senha/i)).toBeInTheDocument();
  });

  it('renders the submit button labelled Entrar', () => {
    render(<LoginForm onLogin={onLogin} />);
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
  });

  it('has data-testid="login-form" on the form element', () => {
    render(<LoginForm onLogin={onLogin} />);
    expect(screen.getByTestId('login-form')).toBeInTheDocument();
  });

  it('email input has type="email"', () => {
    render(<LoginForm onLogin={onLogin} />);
    const emailInput = screen.getByLabelText(/e-?mail/i);
    expect(emailInput).toHaveAttribute('type', 'email');
  });

  it('password input has type="password"', () => {
    render(<LoginForm onLogin={onLogin} />);
    const pwInput = screen.getByLabelText(/senha/i);
    expect(pwInput).toHaveAttribute('type', 'password');
  });

  // ----- Single-org success -----

  it('calls onLogin with session on single-org success', async () => {
    mockedLogin.mockResolvedValueOnce(SINGLE_ORG_SESSION);
    render(<LoginForm onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'dr@aurora.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'pass1234');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(onLogin).toHaveBeenCalledWith(SINGLE_ORG_SESSION.session);
    });
  });

  // ----- Multi-org flow -----

  it('renders org-selection form on multi-org response', async () => {
    mockedLogin.mockResolvedValueOnce(MULTI_ORG_RESPONSE);
    render(<LoginForm onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'dr@aurora.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'pass1234');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByTestId('organization-select-form')).toBeInTheDocument();
    });
    expect(screen.getByText(/clínica aurora/i)).toBeInTheDocument();
    expect(screen.getByText(/clínica horizonte/i)).toBeInTheDocument();
  });

  it('org-selection form has confirm button', async () => {
    mockedLogin.mockResolvedValueOnce(MULTI_ORG_RESPONSE);
    render(<LoginForm onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'dr@aurora.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'pass1234');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /confirmar organização/i })).toBeInTheDocument();
    });
  });

  it('calls onLogin after org selection', async () => {
    mockedLogin.mockResolvedValueOnce(MULTI_ORG_RESPONSE);
    mockedSelectOrg.mockResolvedValueOnce(SINGLE_ORG_SESSION.session);
    render(<LoginForm onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'dr@aurora.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'pass1234');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByTestId('organization-select-form')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /confirmar organização/i }));

    await waitFor(() => {
      expect(onLogin).toHaveBeenCalledWith(SINGLE_ORG_SESSION.session);
    });
  });

  // ----- Error handling -----

  it('displays OperationOutcome alert on login error', async () => {
    const iamError = { body: OPERATION_OUTCOME_ERROR, status: 401 };
    mockedLogin.mockRejectedValueOnce(iamError);
    render(<LoginForm onLogin={onLogin} />);

    await userEvent.type(screen.getByLabelText(/e-?mail/i), 'bad@user.com');
    await userEvent.type(screen.getByLabelText(/senha/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByText(/credenciais inválidas/i)).toBeInTheDocument();
  });

  // ----- AuthTemplate integration -----

  it('renders inside AuthTemplate with auth-template container', () => {
    render(
      <AuthTemplate>
        <LoginForm onLogin={onLogin} />
      </AuthTemplate>
    );
    expect(screen.getByTestId('auth-template')).toBeInTheDocument();
    expect(screen.getByTestId('login-form')).toBeInTheDocument();
  });
});
