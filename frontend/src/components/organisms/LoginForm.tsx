/**
 * T058 [US4] LoginForm organism.
 * T117 [US10] Refined to use AuthFormField molecule (MUI 7 + Tailwind).
 *
 * Handles both single-org and multi-org IAM login flows.
 * On single-org response: calls {@link onLogin} immediately.
 * On multi-org response: renders an inline org-selection step.
 * FHIR OperationOutcome errors are displayed via {@link OperationOutcomeAlert}.
 *
 * Refs: FR-004, FR-013
 */

import { useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import NativeSelect from '@mui/material/NativeSelect';
import Typography from '@mui/material/Typography';
import { OperationOutcomeAlert } from '../molecules/OperationOutcomeAlert';
import { AuthFormField } from '../molecules/AuthFormField';
import {
  login,
  selectOrganization,
  type IamAuthError,
  type OrgOption,
  type SessionIssuedResponse,
} from '../../services/iamAuthApi';
import type { OperationOutcome } from '../../services/clinicRegistrationApi';

export interface LoginFormProps {
  /** Called with the session payload on successful login. */
  onLogin: (session: SessionIssuedResponse) => Promise<void>;
}

const FALLBACK_OUTCOME: OperationOutcome = {
  resourceType: 'OperationOutcome',
  issue: [{ severity: 'error', code: 'exception', diagnostics: 'Erro inesperado. Tente novamente.' }],
};

function toOutcome(err: unknown): OperationOutcome {
  const apiError = err as IamAuthError;
  return apiError?.body ?? FALLBACK_OUTCOME;
}

export function LoginForm({ onLogin }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [outcome, setOutcome] = useState<OperationOutcome | null>(null);

  // Multi-org state
  const [challengeToken, setChallengeToken] = useState<string | null>(null);
  const [organizations, setOrganizations] = useState<OrgOption[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState('');

  async function handleLoginSubmit(event: React.FormEvent) {
    event.preventDefault();
    setOutcome(null);
    setLoading(true);
    try {
      const result = await login({ email, password });
      if (result.mode === 'single') {
        await onLogin(result.session);
      } else {
        setChallengeToken(result.challengeToken);
        setOrganizations(result.organizations);
        setSelectedOrgId(result.organizations[0]?.organizationId ?? '');
      }
    } catch (err) {
      setOutcome(toOutcome(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleOrgSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!challengeToken || !selectedOrgId) return;
    setOutcome(null);
    setLoading(true);
    try {
      const session = await selectOrganization(challengeToken, selectedOrgId);
      await onLogin(session);
    } catch (err) {
      setOutcome(toOutcome(err));
    } finally {
      setLoading(false);
    }
  }

  if (challengeToken) {
    return (
      <Box
        component="form"
        onSubmit={handleOrgSubmit}
        noValidate
        className="flex flex-col gap-4 p-6 rounded-lg bg-white shadow"
        data-testid="organization-select-form"
      >
        <Typography variant="h6" gutterBottom>
          Selecione a organização
        </Typography>

        <FormControl fullWidth>
          <InputLabel variant="standard" htmlFor="org-native-select">
            Organização
          </InputLabel>
          <NativeSelect
            value={selectedOrgId}
            onChange={(e) => setSelectedOrgId(e.target.value)}
            inputProps={{ id: 'org-native-select', 'aria-label': 'Organização' }}
          >
            {organizations.map((org) => (
              <option
                key={org.organizationId}
                value={org.organizationId}
                data-testid={`org-select-${org.displayName}`}
              >
                {org.displayName}
              </option>
            ))}
          </NativeSelect>
        </FormControl>

        <OperationOutcomeAlert outcome={outcome} />

        <Button
          type="submit"
          variant="contained"
          disabled={loading || !selectedOrgId}
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : null}
        >
          {loading ? 'Confirmando…' : 'Confirmar organização'}
        </Button>
      </Box>
    );
  }

  return (
    <Box
      component="form"
      onSubmit={handleLoginSubmit}
      noValidate
      className="flex flex-col gap-4 p-6 rounded-lg bg-white shadow"
      data-testid="login-form"
    >
      <Typography variant="h5" gutterBottom>
        Acesso à plataforma
      </Typography>

      <AuthFormField
        id="login-email"
        label="E-mail"
        type="email"
        value={email}
        onChange={setEmail}
        required
        autoComplete="email"
      />

      <AuthFormField
        id="login-password"
        label="Senha"
        type="password"
        value={password}
        onChange={setPassword}
        required
        autoComplete="current-password"
      />

      <OperationOutcomeAlert outcome={outcome} />

      <Button
        type="submit"
        variant="contained"
        disabled={loading}
        startIcon={loading ? <CircularProgress size={16} color="inherit" /> : null}
      >
        {loading ? 'Entrando…' : 'Entrar'}
      </Button>
    </Box>
  );
}
