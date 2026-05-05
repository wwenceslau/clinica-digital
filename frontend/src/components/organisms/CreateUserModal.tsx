/**
 * T097 CreateUserModal — admin form to create a new profile-20 user.
 *
 * Renders a MUI Dialog with fields for displayName, email, cpf, and password.
 * On submit, calls POST /api/admin/users via createProfile20User().
 * Reports success/error using a toast-style Alert.
 *
 * Props:
 *   open       — whether the dialog is visible
 *   onClose    — callback to close the dialog (with optional boolean flag: true = success)
 *   locationId — UUID of the location to assign the new user to
 *   roleCode   — role code for the practitioner role (e.g. "MD")
 *
 * data-testid markers (consumed by E2E test T092):
 *   btn-create-user  — trigger button (rendered externally, see AdminUsuariosPage)
 *   btn-submit-user  — submit button inside the dialog
 *   toast-success    — success feedback element
 *   toast-error      — error feedback element
 *
 * Refs: FR-006, FR-009, FR-011, FR-013
 */

import React, { useState } from 'react';
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { createProfile20User, type IamAuthError } from '../../services/iamAuthApi';

interface CreateUserModalProps {
  open: boolean;
  onClose: (success?: boolean) => void;
  locationId: string;
  roleCode: string;
}

interface FormState {
  displayName: string;
  email: string;
  cpf: string;
  password: string;
}

const EMPTY_FORM: FormState = {
  displayName: '',
  email: '',
  cpf: '',
  password: '',
};

export function CreateUserModal({
  open,
  onClose,
  locationId,
  roleCode,
}: CreateUserModalProps): React.ReactElement {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  function handleChange(field: keyof FormState) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
    };
  }

  function handleClose() {
    setForm(EMPTY_FORM);
    setSuccessMsg(null);
    setErrorMsg(null);
    onClose();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    try {
      await createProfile20User({
        practitioner: {
          displayName: form.displayName,
          email: form.email,
          cpf: form.cpf,
          password: form.password,
        },
        locationId,
        roleCode,
      });

      setSuccessMsg(`Usuário "${form.displayName}" criado com sucesso.`);
      setForm(EMPTY_FORM);
      // Give the user a moment to see the success message, then close
      setTimeout(() => {
        setSuccessMsg(null);
        onClose(true);
      }, 1500);
    } catch (err: unknown) {
      const authErr = err as IamAuthError;
      const diagnostics =
        authErr?.body?.issue?.[0]?.diagnostics ??
        'Ocorreu um erro ao criar o usuário.';
      setErrorMsg(String(diagnostics));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Criar Novo Usuário</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {successMsg && (
            <Alert severity="success" data-testid="toast-success">
              {successMsg}
            </Alert>
          )}
          {errorMsg && (
            <Alert severity="error" data-testid="toast-error">
              {errorMsg}
            </Alert>
          )}

          <TextField
            label="Nome completo"
            name="displayName"
            value={form.displayName}
            onChange={handleChange('displayName')}
            required
            autoFocus
            fullWidth
            inputProps={{ 'aria-label': 'Nome completo' }}
          />
          <TextField
            label="E-mail"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange('email')}
            required
            fullWidth
          />
          <TextField
            label="CPF (11 dígitos)"
            name="cpf"
            value={form.cpf}
            onChange={handleChange('cpf')}
            required
            fullWidth
            inputProps={{
              'aria-label': 'CPF',
              pattern: '\\d{11}',
              maxLength: 11,
            }}
          />
          <TextField
            label="Senha"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange('password')}
            required
            fullWidth
          />
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose} disabled={loading}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={loading}
            data-testid="btn-submit-user"
            startIcon={loading ? <CircularProgress size={16} /> : null}
          >
            {loading ? 'Salvando…' : 'Criar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
