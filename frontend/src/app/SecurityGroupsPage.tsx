/**
 * SecurityGroupsPage — /admin/security/groups
 *
 * Displays RBAC group management: list all groups, create new groups,
 * manage members, manage permissions, and delete groups.
 *
 * Refs: T177, FR-006
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
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
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  People as PeopleIcon,
  Lock as LockIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { fromCaughtError } from '../services/operationOutcomeAdapter';
import { GroupMembersModal } from '../components/organisms/GroupMembersModal';
import { GroupPermissionsModal } from '../components/organisms/GroupPermissionsModal';

interface IamGroupSummary {
  groupId: string;
  tenantId: string;
  name: string;
  description: string | null;
}

async function listAdminGroups(
  sessionId: string,
  tenantId: string,
): Promise<IamGroupSummary[]> {
  const res = await fetch('/api/admin/groups', {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });

  if (!res.ok) {
    throw new Error(`Erro ao carregar grupos: ${res.status}`);
  }

  return res.json();
}

async function createAdminGroup(
  sessionId: string,
  tenantId: string,
  name: string,
  description: string | null,
): Promise<IamGroupSummary> {
  const res = await fetch('/api/admin/groups', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
    body: JSON.stringify({
      name,
      description: description || null,
    }),
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(
      data.issue?.[0]?.details?.text ||
      `Erro ao criar grupo: ${res.status}`
    );
  }

  return res.json();
}

async function deleteAdminGroup(
  sessionId: string,
  tenantId: string,
  groupId: string,
): Promise<void> {
  const res = await fetch(`/api/admin/groups/${groupId}`, {
    method: 'DELETE',
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(
      data.issue?.[0]?.details?.text ||
      `Erro ao deletar grupo: ${res.status}`
    );
  }
}

export function SecurityGroupsPage(): React.ReactElement {
  const { session } = useAuth();

  const [groups, setGroups] = useState<IamGroupSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Create modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [createLoading, setCreateLoading] = useState(false);

  // Delete confirmation state
  const [deleteTarget, setDeleteTarget] = useState<IamGroupSummary | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Members modal state
  const [membersModalOpen, setMembersModalOpen] = useState(false);
  const [membersTarget, setMembersTarget] = useState<IamGroupSummary | null>(null);

  // Permissions modal state
  const [permissionsModalOpen, setPermissionsModalOpen] = useState(false);
  const [permissionsTarget, setPermissionsTarget] = useState<IamGroupSummary | null>(null);

  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;

    setLoading(true);
    setError(null);

    listAdminGroups(sessionId, tenantId)
      .then(setGroups)
      .catch((e: Error) => {
        const outcome = fromCaughtError(e);
        setError(outcome.userMessage);
      })
      .finally(() => setLoading(false));
  }, [sessionId, tenantId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleCreate() {
    if (!createName.trim() || !sessionId || !tenantId) {
      setError('Nome do grupo é obrigatório');
      return;
    }

    setCreateLoading(true);
    setError(null);

    try {
      const newGroup = await createAdminGroup(
        sessionId,
        tenantId,
        createName.trim(),
        createDescription.trim() || null
      );

      setGroups([...groups, newGroup]);
      setCreateModalOpen(false);
      setCreateName('');
      setCreateDescription('');
    } catch (e: unknown) {
      const outcome = fromCaughtError(e);
      setError(outcome.userMessage);
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget || !sessionId || !tenantId) return;

    setDeleteLoading(true);
    setError(null);

    try {
      await deleteAdminGroup(sessionId, tenantId, deleteTarget.groupId);
      setGroups(groups.filter((g) => g.groupId !== deleteTarget.groupId));
      setDeleteTarget(null);
    } catch (e: unknown) {
      const outcome = fromCaughtError(e);
      setError(outcome.userMessage);
    } finally {
      setDeleteLoading(false);
    }
  }

  function handleMembersOpen(group: IamGroupSummary) {
    setMembersTarget(group);
    setMembersModalOpen(true);
  }

  function handleMembersClose(refreshed?: boolean) {
    setMembersModalOpen(false);
    setMembersTarget(null);
    if (refreshed) {
      load();
    }
  }

  function handlePermissionsOpen(group: IamGroupSummary) {
    setPermissionsTarget(group);
    setPermissionsModalOpen(true);
  }

  function handlePermissionsClose(refreshed?: boolean) {
    setPermissionsModalOpen(false);
    setPermissionsTarget(null);
    if (refreshed) {
      load();
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          Gestão de Grupos
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateModalOpen(true)}
            data-testid="btn-create-group"
          >
            Novo Grupo
          </Button>
          <Tooltip title="Atualizar">
            <IconButton onClick={load} disabled={loading} aria-label="atualizar grupos">
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label="grupos RBAC">
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.50' }}>
              <TableCell><strong>Nome</strong></TableCell>
              <TableCell><strong>Descrição</strong></TableCell>
              <TableCell align="center"><strong>Ações</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {groups.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Nenhum grupo encontrado.
                </TableCell>
              </TableRow>
            ) : (
              groups.map((group) => (
                <TableRow key={group.groupId} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {group.name}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: 12, color: 'text.secondary', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {group.description ?? '—'}
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="Gerenciar membros">
                      <IconButton
                        size="small"
                        onClick={() => handleMembersOpen(group)}
                        data-testid={`btn-members-${group.groupId}`}
                      >
                        <PeopleIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Gerenciar permissões">
                      <IconButton
                        size="small"
                        onClick={() => handlePermissionsOpen(group)}
                        data-testid={`btn-permissions-${group.groupId}`}
                      >
                        <LockIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Deletar grupo">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => setDeleteTarget(group)}
                        data-testid={`btn-delete-${group.groupId}`}
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

      {/* Create Group Modal */}
      <Dialog
        open={createModalOpen}
        onClose={() => !createLoading && setCreateModalOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Novo Grupo</DialogTitle>
        <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Nome do grupo"
            placeholder="ex: Enfermeiros"
            value={createName}
            onChange={(e) => setCreateName(e.target.value)}
            disabled={createLoading}
            autoFocus
            fullWidth
          />
          <TextField
            label="Descrição (opcional)"
            placeholder="ex: Grupo de enfermeiros da unidade"
            value={createDescription}
            onChange={(e) => setCreateDescription(e.target.value)}
            disabled={createLoading}
            multiline
            rows={3}
            fullWidth
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateModalOpen(false)} disabled={createLoading}>
            Cancelar
          </Button>
          <Button
            onClick={handleCreate}
            disabled={!createName.trim() || createLoading}
            variant="contained"
          >
            {createLoading ? <CircularProgress size={16} sx={{ mr: 1 }} /> : null}
            Criar
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={deleteTarget !== null} onClose={() => !deleteLoading && setDeleteTarget(null)}>
        <DialogTitle>Confirmar exclusão</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Tem certeza que deseja deletar o grupo <strong>{deleteTarget?.name}</strong>? Esta ação não pode ser desfeita.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={deleteLoading}>
            Cancelar
          </Button>
          <Button
            onClick={handleDelete}
            disabled={deleteLoading}
            color="error"
            variant="contained"
          >
            {deleteLoading ? <CircularProgress size={16} sx={{ mr: 1 }} /> : null}
            Deletar
          </Button>
        </DialogActions>
      </Dialog>

      {/* Members Modal */}
      {membersTarget && (
        <GroupMembersModal
          open={membersModalOpen}
          group={membersTarget}
          onClose={handleMembersClose}
        />
      )}

      {/* Permissions Modal */}
      {permissionsTarget && (
        <GroupPermissionsModal
          open={permissionsModalOpen}
          group={permissionsTarget}
          onClose={handlePermissionsClose}
        />
      )}
    </Box>
  );
}
