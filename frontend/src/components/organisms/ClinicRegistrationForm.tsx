/**
 * T046 [US3] ClinicRegistrationForm organism.
 * T117 [US10] Confirmed as registration organism in atomic design pattern.
 *
 * Public form for registering a new clinic (tenant) and its first admin.
 * Follows the atomic design pattern (organism) with MUI 7 + Tailwind classes.
 *
 * On successful submission, calls {@link onSuccess} with the server response.
 * On error (4xx), displays a FHIR OperationOutcome alert via
 * {@link OperationOutcomeAlert}.
 *
 * Refs: FR-003, FR-009, FR-013
 */

import { useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import { OperationOutcomeAlert } from '../molecules/OperationOutcomeAlert';
import {
  registerClinic,
  type ClinicRegistrationError,
  type ClinicRegistrationResponse,
  type OperationOutcome,
} from '../../services/clinicRegistrationApi';

export interface ClinicRegistrationFormProps {
  onSuccess: (response: ClinicRegistrationResponse) => void;
}

export function ClinicRegistrationForm({ onSuccess }: ClinicRegistrationFormProps) {
  const [displayName, setDisplayName] = useState('');
  const [cnes, setCnes] = useState('');
  const [email, setEmail] = useState('');
  const [cpf, setCpf] = useState('');
  const [password, setPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [outcome, setOutcome] = useState<OperationOutcome | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setOutcome(null);
    setSuccessMessage(null);
    setLoading(true);

    try {
      const response = await registerClinic({
        organization: { displayName, cnes },
        adminPractitioner: { displayName, email, cpf, password },
      });

      setSuccessMessage('Clínica cadastrada com sucesso!');
      onSuccess(response);
    } catch (err) {
      const apiError = err as ClinicRegistrationError;
      if (apiError?.body) {
        setOutcome(apiError.body);
      } else {
        setOutcome({
          resourceType: 'OperationOutcome',
          issue: [{ severity: 'error', code: 'exception', diagnostics: 'Erro inesperado. Tente novamente.' }],
        });
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      className="flex flex-col gap-4 p-6 rounded-lg bg-white shadow"
      data-testid="clinic-registration-form"
    >
      <Typography variant="h5" component="h1" className="font-semibold">
        Registrar Clínica
      </Typography>

      <TextField
        id="displayName"
        label="Nome da Clínica"
        name="displayName"
        value={displayName}
        onChange={(e) => setDisplayName(e.target.value)}
        required
        fullWidth
        inputProps={{ 'aria-label': 'Nome da Clínica' }}
      />

      <TextField
        id="cnes"
        label="CNES"
        name="cnes"
        value={cnes}
        onChange={(e) => setCnes(e.target.value)}
        required
        fullWidth
        inputProps={{ 'aria-label': 'CNES', maxLength: 7 }}
        helperText="7 dígitos numéricos"
      />

      <TextField
        id="email"
        label="E-mail do Administrador"
        name="email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        fullWidth
        inputProps={{ 'aria-label': 'E-mail' }}
      />

      <TextField
        id="cpf"
        label="CPF do Administrador"
        name="cpf"
        value={cpf}
        onChange={(e) => setCpf(e.target.value)}
        required
        fullWidth
        inputProps={{ 'aria-label': 'CPF', maxLength: 11 }}
        helperText="11 dígitos numéricos, sem pontuação"
      />

      <TextField
        id="password"
        label="Senha"
        name="password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
        fullWidth
        inputProps={{ 'aria-label': 'Senha' }}
      />

      {successMessage && (
        <Alert severity="success" role="alert">
          {successMessage}
        </Alert>
      )}

      <OperationOutcomeAlert outcome={outcome} />

      <Button
        type="submit"
        variant="contained"
        color="primary"
        disabled={loading}
        fullWidth
        className="mt-2"
      >
        {loading ? <CircularProgress size={24} color="inherit" /> : 'Registrar'}
      </Button>
    </Box>
  );
}
