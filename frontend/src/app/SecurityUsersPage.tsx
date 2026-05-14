/**
 * SecurityUsersPage — /admin/security/users
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import { Refresh as RefreshIcon, Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useTranslation } from 'react-i18next';
import { CreateUserModal, type UserLocationOption, type UserRoleOption } from '../components/organisms/CreateUserModal';
import { useAuth } from '../context/AuthContext';
import { listAdminLocations } from '../services/adminLocationApi';

import { fromCaughtError } from '../services/operationOutcomeAdapter';

interface TenantUserSummary {
  id: string;
  email: string;
  username: string;
  profileType: number;
  accountActive: boolean;
  createdAt: string | null;
}

async function listAdminUsers(
  sessionId: string,
  tenantId: string,
  errorMessage: (status: number) => string,
): Promise<TenantUserSummary[]> {
  const res = await fetch('/api/admin/users', {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });

  if (!res.ok) {
    throw new Error(errorMessage(res.status));
  }

  return res.json();
}

const CBO_OPTIONS: UserRoleOption[] = [
  { code: 'CBO-225125', label: 'CBO-225125 — Médico Clínico' },
  { code: 'CBO-225142', label: 'CBO-225142 — Médico de Família e Comunidade' },
  { code: 'CBO-225320', label: 'CBO-225320 — Médico Pediatra' },
  { code: 'CBO-225250', label: 'CBO-225250 — Médico Cardiologista' },
  { code: 'CBO-225265', label: 'CBO-225265 — Médico Psiquiatra' },
  { code: 'CBO-225230', label: 'CBO-225230 — Médico Ginecologista e Obstetra' },
  { code: 'CBO-225360', label: 'CBO-225360 — Médico Ortopedista' },
  { code: 'CBO-225170', label: 'CBO-225170 — Médico Cirurgião Geral' },
  { code: 'CBO-223505', label: 'CBO-223505 — Enfermeiro' },
  { code: 'CBO-322405', label: 'CBO-322405 — Técnico em Enfermagem' },
  { code: 'CBO-223910', label: 'CBO-223910 — Fisioterapeuta' },
  { code: 'CBO-226305', label: 'CBO-226305 — Farmacêutico' },
  { code: 'CBO-223208', label: 'CBO-223208 — Nutricionista' },
  { code: 'CBO-226315', label: 'CBO-226315 — Cirurgião-Dentista' },
  { code: 'CBO-251510', label: 'CBO-251510 — Psicólogo Clínico' },
  { code: 'CBO-251605', label: 'CBO-251605 — Assistente Social' },
  { code: 'CBO-324205', label: 'CBO-324205 — Técnico em Radiologia' },
  { code: 'CBO-223605', label: 'CBO-223605 — Fonoaudiólogo' },
  { code: 'CBO-223305', label: 'CBO-223305 — Terapeuta Ocupacional' },
  { code: 'CBO-223415', label: 'CBO-223415 — Biomédico' },
];

export function SecurityUsersPage(): React.ReactElement {
  const { t } = useTranslation();
  const { session } = useAuth();

  const [users, setUsers] = useState<TenantUserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<TenantUserSummary | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantUserSummary | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [locationOptions, setLocationOptions] = useState<UserLocationOption[]>([]);
  const [roleOptions, setRoleOptions] = useState<UserRoleOption[]>([]);

  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';
  const SYSTEM_TENANT_ID = '00000000-0000-0000-0000-000000000000';
  const isTenantAdmin = !!tenantId && tenantId !== SYSTEM_TENANT_ID;

  const profileLabel = (profileType: number): string => {
    const map: Record<number, string> = {
      0: t('securityUsers.profile.superuser'),
      10: t('securityUsers.profile.admin'),
      20: t('securityUsers.profile.professional'),
    };

    return map[profileType] ?? t('securityUsers.profile.unknown', { code: profileType });
  };

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;

    setLoading(true);
    setError(null);

    listAdminUsers(sessionId, tenantId, (status) => t('securityUsers.error.load', { status }))
      .then((data) => setUsers(data.filter((user) => user.profileType !== 0)))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [sessionId, tenantId, t]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!isTenantAdmin) return;
    setRoleOptions(CBO_OPTIONS);
    listAdminLocations(tenantId, sessionId)
      .then((locations) => {
        setLocationOptions(
          locations
            .filter((loc) => loc.accountActive)
            .map((loc) => ({ id: loc.id, label: loc.displayName })),
        );
      })
      .catch((e: unknown) => {
        const outcome = fromCaughtError(e);
        setError(outcome.userMessage);
      });
  }, [isTenantAdmin, tenantId, sessionId]);

  function handleModalClose(success?: boolean) {
    setModalOpen(false);
    setEditTarget(null);
    if (success) {
      load();
    }
  }

  async function handleDelete() {
    if (!deleteTarget || !sessionId || !tenantId) return;

    setDeleteLoading(true);
    setError(null);

    try {
      const res = await fetch(`/api/admin/users/${deleteTarget.id}`, {
        method: 'DELETE',
        headers: {
          'X-Tenant-ID': tenantId,
          Authorization: `Bearer ${sessionId}`,
        },
      });

      if (!res.ok) {
        throw new Error(t('securityUsers.error.delete', { status: res.status }));
      }

      setDeleteTarget(null);
      load();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : t('securityUsers.error.delete', { status: 500 }));
    } finally {
      setDeleteLoading(false);
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          {t('securityUsers.title')}
        </Typography>

        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title={t('securityUsers.actions.refresh')}>
            <IconButton
              onClick={load}
              disabled={loading || !tenantId}
              aria-label={t('securityUsers.actions.refreshAriaLabel')}
            >
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>

          {isTenantAdmin && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setEditTarget(null);
                setModalOpen(true);
              }}
              data-testid="btn-create-user"
            >
              {t('securityUsers.actions.newUser')}
            </Button>
          )}
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label={t('securityUsers.grid.ariaLabel')}>
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.50' }}>
              <TableCell><strong>{t('securityUsers.grid.email')}</strong></TableCell>
              <TableCell><strong>{t('securityUsers.grid.username')}</strong></TableCell>
              <TableCell><strong>{t('securityUsers.grid.profile')}</strong></TableCell>
              <TableCell><strong>{t('securityUsers.grid.status')}</strong></TableCell>
              <TableCell><strong>{t('securityUsers.grid.createdAt')}</strong></TableCell>
              <TableCell><strong>{t('securityUsers.grid.actions')}</strong></TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {users.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  {t('securityUsers.grid.empty')}
                </TableCell>
              </TableRow>
            ) : (
              users.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>{u.username}</TableCell>
                  <TableCell>
                    <Chip
                      label={profileLabel(u.profileType)}
                      size="small"
                      color={u.profileType === 10 ? 'primary' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={u.accountActive ? t('securityUsers.status.active') : t('securityUsers.status.inactive')}
                      size="small"
                      color={u.accountActive ? 'success' : 'error'}
                    />
                  </TableCell>
                  <TableCell>
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString('pt-BR') : '-'}
                  </TableCell>
                  <TableCell>
                    <Tooltip title={t('securityUsers.actions.edit')}>
                      <IconButton
                        size="small"
                        onClick={() => {
                          setModalOpen(false);
                          setEditTarget(u);
                        }}
                        aria-label={t('securityUsers.actions.edit')}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('securityUsers.actions.delete')}>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => setDeleteTarget(u)}
                        aria-label={t('securityUsers.actions.delete')}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <CreateUserModal
        open={modalOpen || editTarget != null}
        onClose={handleModalClose}
        locationId={locationOptions[0]?.id ?? ''}
        roleCode={roleOptions[0]?.code ?? ''}
        locationOptions={locationOptions}
        roleOptions={roleOptions}
        editUser={
          editTarget
            ? {
                id: editTarget.id,
                email: editTarget.email,
                username: editTarget.username,
                accountActive: editTarget.accountActive,
              }
            : undefined
        }
      />

      <Dialog open={deleteTarget != null} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>{t('securityUsers.deleteDialog.title')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('securityUsers.deleteDialog.message', { name: deleteTarget?.email ?? '' })}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={deleteLoading}>
            {t('securityUsers.deleteDialog.cancel')}
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={deleteLoading}
            onClick={handleDelete}
          >
            {deleteLoading ? <CircularProgress size={16} /> : t('securityUsers.deleteDialog.confirm')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
