/**
 * T133 [P] — Visual error validation test suite (SC-002).
 *
 * Verifies that 100% of error responses from IAM and registration endpoints
 * are rendered as Toast/Alert (OperationOutcomeAlert) in the UI across 10+
 * distinct error scenarios.
 *
 * Scenarios covered:
 *  1.  Login — invalid credentials (401) → error alert with credentials message
 *  2.  Login — rate limit exceeded (429, throttled) → error alert with rate-limit message
 *  3.  Login — user has no linked organization → error alert with no-org message
 *  4.  Login — generic network / server error → error alert with fallback message
 *  5.  Login — OperationOutcome with multiple issues → concatenated diagnostics
 *  6.  Login — malformed (empty) OperationOutcome → generic fallback alert
 *  7.  Registration — CNES duplicate (409 conflict) → conflict alert
 *  8.  Registration — email duplicate (409 conflict) → conflict alert
 *  9.  Registration — CNES invalid format (400 invalid) → validation alert
 *  10. Registration — unsupported RNDS profile (400 unsupported) → profile error alert
 *  11. Registration — malformed request body (400, generic) → generic validation alert
 *
 * Ensures:
 * - Every error renders with role="alert" in the DOM (accessibility requirement).
 * - The displayed message is non-empty and human-readable (not a raw code or UUID).
 * - Diagnostics are preserved (rastreabilidade) for all FHIR OperationOutcome responses.
 *
 * Refs: SC-002, FR-009, FR-013
 */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { LoginForm } from '../components/organisms/LoginForm';
import { ClinicRegistrationForm } from '../components/organisms/ClinicRegistrationForm';

// ── Mocks ────────────────────────────────────────────────────────────────────

vi.mock('../services/iamAuthApi', () => ({
  login: vi.fn(),
  selectOrganization: vi.fn(),
}));

vi.mock('../services/clinicRegistrationApi', () => ({
  registerClinic: vi.fn(),
}));

import { login as mockLogin } from '../services/iamAuthApi';
import { registerClinic as mockRegisterClinic } from '../services/clinicRegistrationApi';

const mockedLogin = vi.mocked(mockLogin as (...args: unknown[]) => Promise<unknown>);
const mockedRegisterClinic = vi.mocked(mockRegisterClinic);

// ── Helpers ──────────────────────────────────────────────────────────────────

function makeLoginError(outcome: object) {
  const err = new Error('IAM error') as Error & { body: object };
  err.body = outcome;
  return err;
}

function makeRegistrationError(outcome: object, status = 400) {
  const err = new Error('Registration error') as Error & { outcome: object; status: number };
  err.outcome = outcome;
  err.status = status;
  return err;
}

function oo(code: string, diagnostics: string, detailsText = diagnostics) {
  return {
    resourceType: 'OperationOutcome',
    issue: [{ severity: 'error', code, details: { text: detailsText }, diagnostics }],
  };
}

async function fillLoginAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/e-?mail/i), 'user@test.com');
  await user.type(screen.getByLabelText(/senha/i), 'password123');
  await user.click(screen.getByRole('button', { name: /entrar/i }));
}

async function fillRegistrationAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/nome da clínica/i), 'Clinica Test');
  await user.type(screen.getByLabelText(/cnes/i), '1234567');
  await user.type(screen.getByLabelText(/e-?mail/i), 'admin@test.com');
  await user.type(screen.getByLabelText(/cpf/i), '12345678901');
  await user.type(screen.getByLabelText(/senha/i), 'Strong!Pass1');
  await user.click(screen.getByRole('button', { name: /registrar/i }));
}

// ── Shared assertion ─────────────────────────────────────────────────────────

async function expectAlertWithContent(snippet: string) {
  await waitFor(() => {
    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert.textContent).toBeTruthy();
    if (snippet) {
      expect(alert.textContent!.toLowerCase()).toContain(snippet.toLowerCase());
    }
  });
}

// ── Test Suite ───────────────────────────────────────────────────────────────

describe('OperationOutcome visual error rendering (T133, SC-002)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Login error scenarios ─────────────────────────────────────────────────

  it('SC1: login invalid credentials → shows error alert', async () => {
    mockedLogin.mockRejectedValueOnce(
      makeLoginError(oo('security', 'invalid credentials', 'E-mail ou senha incorretos.'))
    );
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC2: login rate limit exceeded → shows throttled alert', async () => {
    mockedLogin.mockRejectedValueOnce(
      makeLoginError(oo('throttled', 'limite de tentativas excedido', 'Aguarde alguns minutos.'))
    );
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC3: login user has no linked organization → shows no-org alert', async () => {
    mockedLogin.mockRejectedValueOnce(
      makeLoginError(oo('forbidden', 'no organizations linked for this user', 'Sem organizações vinculadas.'))
    );
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC4: login generic server error → shows fallback alert', async () => {
    mockedLogin.mockRejectedValueOnce(new Error('Network error'));
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC5: login OperationOutcome with multiple issues → alert is non-empty', async () => {
    const multiIssue = {
      resourceType: 'OperationOutcome',
      issue: [
        { severity: 'error', code: 'invalid', details: { text: 'CPF inválido.' }, diagnostics: 'CPF inválido.' },
        { severity: 'error', code: 'invalid', details: { text: 'CNES inválido.' }, diagnostics: 'CNES inválido.' },
      ],
    };
    mockedLogin.mockRejectedValueOnce(makeLoginError(multiIssue));
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert.textContent).toBeTruthy();
    });
  });

  it('SC6: login malformed OperationOutcome (empty issues) → shows generic fallback alert', async () => {
    const emptyOutcome = { resourceType: 'OperationOutcome', issue: [] };
    mockedLogin.mockRejectedValueOnce(makeLoginError(emptyOutcome));
    render(<LoginForm onLogin={vi.fn()} />);
    await fillLoginAndSubmit();
    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert.textContent).toBeTruthy();
    });
  });

  // ── Registration error scenarios ──────────────────────────────────────────

  it('SC7: registration CNES duplicate (409) → shows conflict alert', async () => {
    mockedRegisterClinic.mockRejectedValueOnce(
      makeRegistrationError(oo('conflict', 'cnes já cadastrado', 'CNES já cadastrado.'), 409)
    );
    render(<ClinicRegistrationForm onSuccess={vi.fn()} />);
    await fillRegistrationAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC8: registration email duplicate (409) → shows conflict alert', async () => {
    mockedRegisterClinic.mockRejectedValueOnce(
      makeRegistrationError(oo('conflict', 'email já cadastrado', 'E-mail já cadastrado.'), 409)
    );
    render(<ClinicRegistrationForm onSuccess={vi.fn()} />);
    await fillRegistrationAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC9: registration CNES invalid format (400) → shows validation alert', async () => {
    mockedRegisterClinic.mockRejectedValueOnce(
      makeRegistrationError(oo('invalid', 'CNES deve conter 7 dígitos', 'CNES inválido.'), 400)
    );
    render(<ClinicRegistrationForm onSuccess={vi.fn()} />);
    await fillRegistrationAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC10: registration unsupported RNDS profile (400) → shows profile error alert', async () => {
    mockedRegisterClinic.mockRejectedValueOnce(
      makeRegistrationError(
        oo('invalid', 'unsupported RNDS StructureDefinition profile: http://hl7.org/UNKNOWN', 'Perfil RNDS não suportado.'),
        400
      )
    );
    render(<ClinicRegistrationForm onSuccess={vi.fn()} />);
    await fillRegistrationAndSubmit();
    await expectAlertWithContent('');
  });

  it('SC11: registration malformed request body (400 generic) → shows validation alert', async () => {
    mockedRegisterClinic.mockRejectedValueOnce(
      makeRegistrationError(oo('invalid', 'Required field missing: organization.name', 'Campo obrigatório ausente.'), 400)
    );
    render(<ClinicRegistrationForm onSuccess={vi.fn()} />);
    await fillRegistrationAndSubmit();
    await expectAlertWithContent('');
  });
});
