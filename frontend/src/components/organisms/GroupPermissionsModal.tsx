/**
 * GroupPermissionsModal — Dialog for managing group permissions (list and remove).
 *
 * Displays current permissions assigned to a group and allows removing them.
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

interface IamPermissionResult {
  permissionId: string;
  code: string;
  description: string | null;
}

interface GroupPermissionsModalProps {
  open: boolean;
  group: IamGroupSummary;
  onClose: (refreshed?: boolean) => void;
}

async function listGroupPermissions(
  sessionId: string,
  tenantId: string,
  groupId: string,
): Promise<IamPermissionResult[]> {
  const res = await fetch(`/api/admin/groups/${groupId}/permissions`, {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });

  if (!res.ok) {
    throw new Error(`Erro ao carregar permissões: ${res.status}`);
  }

  return res.json();
}

async function removePermissionFromGroup(
  sessionId: string,
  tenantId: string,
  groupId: string,
  permissionId: string,
): Promise<void> {
  const res = await fetch(`/api/admin/groups/${groupId}/permissions/${permissionId}`, {
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
      `Erro ao remover permissão: ${res.status}`
    );
  }
}

export function GroupPermissionsModal({ open, group, onClose }: GroupPermissionsModalProps): React.ReactElement {
  const { session } = useAuth();

  const [permissions, setPermissions] = useState<IamPermissionResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [removeTarget, setRemoveTarget] = useState<IamPermissionResult | null>(null);
  const [removeLoading, setRemoveLoading] = useState(false);

  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;

    setLoading(true);
    setError(null);

    listGroupPermissions(sessionId, tenantId, group.groupId)
      .then(setPermissions)
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
      await removePermissionFromGroup(sessionId, tenantId, group.groupId, removeTarget.permissionId);
      setPermissions(permissions.filter((p) => p.permissionId !== removeTarget.permissionId));
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
      <DialogTitle>Permissões: {group.name}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="body2" color="text.secondary">
            {permissions.length} permissão(ões)
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
                <TableCell><strong>Código</strong></TableCell>
                <TableCell><strong>Descrição</strong></TableCell>
                <TableCell align="center"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {permissions.length === 0 && !loading ? (
                <TableRow>
                  <TableCell colSpan={3} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    Nenhuma permissão neste grupo.
                  </TableCell>
                </TableRow>
              ) : (
                permissions.map((permission) => (
                  <TableRow key={permission.permissionId} hover>
                    <TableCell sx={{ fontSize: 12, fontFamily: 'monospace' }}>
                      {permission.code}
                    </TableCell>
                    <TableCell sx={{ fontSize: 12, color: 'text.secondary', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {permission.description ?? '—'}
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Remover do grupo">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => setRemoveTarget(permission)}
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

      {/* Remove Permission Confirmation */}
      <Dialog open={removeTarget !== null} onClose={() => !removeLoading && setRemoveTarget(null)}>
        <DialogTitle>Remover permissão</DialogTitle>
        <DialogContent>
          <Typography>
            Tem certeza que deseja remover a permissão <strong>{removeTarget?.code}</strong> do grupo?
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
