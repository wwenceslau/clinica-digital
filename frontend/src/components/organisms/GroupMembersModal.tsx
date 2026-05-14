/**
 * GroupMembersModal — Dialog for managing group members (list and remove).
 *
 * Displays current members of a group and allows removing them.
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
import { Delete as DeleteIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { fromCaughtError } from '../../services/operationOutcomeAdapter';

interface IamGroupSummary {
  groupId: string;
  tenantId: string;
  name: string;
  description: string | null;
}

interface IamUserResult {
  userId: string;
  username: string;
  email: string;
}

interface GroupMembersModalProps {
  open: boolean;
  group: IamGroupSummary;
  onClose: (refreshed?: boolean) => void;
}

async function listGroupMembers(
  sessionId: string,
  tenantId: string,
  groupId: string,
): Promise<IamUserResult[]> {
  const res = await fetch(`/api/admin/groups/${groupId}/members`, {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });

  if (!res.ok) {
    throw new Error(`Erro ao carregar membros: ${res.status}`);
  }

  return res.json();
}

async function removeMemberFromGroup(
  sessionId: string,
  tenantId: string,
  groupId: string,
  userId: string,
): Promise<void> {
  const res = await fetch(`/api/admin/groups/${groupId}/members/${userId}`, {
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
      `Erro ao remover membro: ${res.status}`
    );
  }
}

export function GroupMembersModal({ open, group, onClose }: GroupMembersModalProps): React.ReactElement {
  const { session } = useAuth();

  const [members, setMembers] = useState<IamUserResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [removeTarget, setRemoveTarget] = useState<IamUserResult | null>(null);
  const [removeLoading, setRemoveLoading] = useState(false);

  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;

    setLoading(true);
    setError(null);

    listGroupMembers(sessionId, tenantId, group.groupId)
      .then(setMembers)
      .catch((e: Error) => {
        const outcome = fromCaughtError(e);
        setError(outcome.userMessage);
      })
      .finally(() => setLoading(false));
  }, [sessionId, tenantId, group.groupId]);

  useEffect(() => {
    if (open) {
      load();
    }
  }, [open, load]);

  async function handleRemove() {
    if (!removeTarget || !sessionId || !tenantId) return;

    setRemoveLoading(true);
    setError(null);

    try {
      await removeMemberFromGroup(sessionId, tenantId, group.groupId, removeTarget.userId);
      setMembers(members.filter((m) => m.userId !== removeTarget.userId));
      setRemoveTarget(null);
    } catch (e: unknown) {
      const outcome = fromCaughtError(e);
      setError(outcome.userMessage);
    } finally {
      setRemoveLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={() => !loading && !removeLoading && onClose()} maxWidth="sm" fullWidth>
      <DialogTitle>Membros: {group.name}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="body2" color="text.secondary">
            {members.length} membro(s)
          </Typography>
          <Tooltip title="Atualizar">
            <IconButton onClick={load} disabled={loading} size="small">
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>
        </Box>

        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
                <TableCell><strong>Usuário</strong></TableCell>
                <TableCell><strong>Email</strong></TableCell>
                <TableCell align="center"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {members.length === 0 && !loading ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    Nenhum membro neste grupo.
                  </TableCell>
                </TableRow>
              ) : (
                members.map((member) => (
                  <TableRow key={member.userId} hover>
                    <TableCell sx={{ fontSize: 12 }}>
                      {member.username}
                    </TableCell>
                    <TableCell sx={{ fontSize: 12, color: 'text.secondary' }}>
                      {member.email}
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Remover do grupo">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => setRemoveTarget(member)}
                          disabled={removeLoading}
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
      </DialogContent>
      <DialogActions>
        <Button onClick={() => onClose()} disabled={loading || removeLoading}>
          Fechar
        </Button>
      </DialogActions>

      {/* Remove Member Confirmation */}
      <Dialog open={removeTarget !== null} onClose={() => !removeLoading && setRemoveTarget(null)}>
        <DialogTitle>Remover membro</DialogTitle>
        <DialogContent>
          <Typography>
            Tem certeza que deseja remover <strong>{removeTarget?.username}</strong> do grupo?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRemoveTarget(null)} disabled={removeLoading}>
            Cancelar
          </Button>
          <Button onClick={handleRemove} disabled={removeLoading} color="error" variant="contained">
            {removeLoading ? <CircularProgress size={16} sx={{ mr: 1 }} /> : null}
            Remover
          </Button>
        </DialogActions>
      </Dialog>
    </Dialog>
  );
}
