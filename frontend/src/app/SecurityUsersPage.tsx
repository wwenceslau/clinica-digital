/**
 * SecurityUsersPage — /admin/security/users
 *
 * Lists all profile-20 users in the tenant and allows creating new ones.
 * Calls GET /api/admin/users and delegates creation to CreateUserModal.
 *
 * Refs: T098, T097, FR-006, FR-013
 */
import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
import { Refresh as RefreshIcon, Add as AddIcon } from '@mui/icons-material';
import { CreateUserModal } from '../components/organisms/CreateUserModal';
import { useAuth } from '../context/AuthContext';

interface TenantUserSummary {
  id: string;
  email: string;
  username: string;
  profileType: number;
  accountActive: boolean;
  createdAt: string | null;
}

const PROFILE_LABELS: Record<number, string> = {
  0: 'Super-user',
  10: 'Admin',
  20: 'Profissional',
};

async function listAdminUsers(sessionId: string, tenantId: string): Promise<TenantUserSummary[]> {
  const res = await fetch('/api/admin/users', {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });
  if (!res.ok) {
    throw new Error(`Erro ao carregar usuários: ${res.status}`);
  }
  return res.json();
}

export function SecurityUsersPage(): React.ReactElement {
  const { session } = useAuth();
  const [users, setUsers] = useState<TenantUserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;
    setLoading(true);
    setError(null);
    listAdminUsers(sessionId, tenantId)
      .then(setUsers)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [sessionId, tenantId]);

  useEffect(() => {
    load();
  }, [load]);

  function handleModalClose(success?: boolean) {
    setModalOpen(false);
    if (success) load();
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          Usuários do Tenant
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Atualizar">
            <IconButton onClick={load} disabled={loading} aria-label="atualizar lista">
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setModalOpen(true)}
            data-testid="btn-create-user"
          >
            Novo Usuário
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label="lista de usuários do tenant">
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.50' }}>
              <TableCell><strong>E-mail</strong></TableCell>
              <TableCell><strong>Nome de usuário</strong></TableCell>
              <TableCell><strong>Perfil</strong></TableCell>
              <TableCell><strong>Status</strong></TableCell>
              <TableCell><strong>Criado em</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Nenhum usuário cadastrado neste tenant.
                </TableCell>
              </TableRow>
            ) : (
              users.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>{u.username}</TableCell>
                  <TableCell>
                    <Chip
                      label={PROFILE_LABELS[u.profileType] ?? `Perfil ${u.profileType}`}
                      size="small"
                      color={u.profileType === 10 ? 'primary' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={u.accountActive ? 'Ativo' : 'Inativo'}
                      size="small"
                      color={u.accountActive ? 'success' : 'error'}
                    />
                  </TableCell>
                  <TableCell>
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString('pt-BR') : '—'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <CreateUserModal
        open={modalOpen}
        onClose={handleModalClose}
        locationId=""
        roleCode="CBO-251510"
      />
    </Box>
  );
}
