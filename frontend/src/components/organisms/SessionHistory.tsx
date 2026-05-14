/**
 * T058 [US4] SessionHistory organism.
 *
 * Displays a tabular audit trail of recent IAM session events.
 *
 * Refs: FR-004
 */

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import TextField from '@mui/material/TextField';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';

const SYSTEM_TENANT_ID = '00000000-0000-0000-0000-000000000000';

export interface SessionItem {
  id: string;
  userId: string;
  issuedAt: string | null;
  expiresAt: string | null;
  revokedAt: string | null;
  revocationReason: string | null;
  active: boolean;
  traceId: string;
}

interface RevokePayload {
  revocationReason: string;
}

async function fetchSessions(sessionId: string, tenantId: string): Promise<SessionItem[]> {
  const res = await fetch('/api/admin/sessions?limit=100', {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });
  if (!res.ok) {
    throw new Error(`Erro ao carregar sessões: ${res.status}`);
  }
  return res.json();
}

async function revokeSession(
  sessionId: string,
  tenantId: string,
  targetSessionId: string,
  payload: RevokePayload,
): Promise<void> {
  const res = await fetch(`/api/admin/sessions/${targetSessionId}/revoke`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    throw new Error(`Erro ao revogar sessão: ${res.status}`);
  }
}

export function SessionHistory() {
  const { session } = useAuth();
  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const [sessions, setSessions] = useState<SessionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [revocationReason, setRevocationReason] = useState('admin_revoke');

  const load = useCallback(() => {
    if (!sessionId || !tenantId || tenantId === SYSTEM_TENANT_ID) return;
    setLoading(true);
    setError(null);
    fetchSessions(sessionId, tenantId)
      .then(setSessions)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [sessionId, tenantId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleRevoke(targetSessionId: string) {
    try {
      await revokeSession(sessionId, tenantId, targetSessionId, { revocationReason: revocationReason.trim() || 'admin_revoke' });
      load();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Falha ao revogar sessão.');
    }
  }

  return (
    <Box data-testid="session-history">
      <Typography variant="h6" gutterBottom>
        Histórico de sessões
      </Typography>
      <Box sx={{ mb: 2, display: 'flex', gap: 1, alignItems: 'center' }}>
        <TextField
          size="small"
          label="Motivo da revogação"
          value={revocationReason}
          onChange={(e) => setRevocationReason(e.target.value)}
        />
        <Button variant="outlined" onClick={load} disabled={loading}>
          {loading ? <CircularProgress size={16} /> : 'Atualizar'}
        </Button>
      </Box>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Usuário</TableCell>
              <TableCell>Emitida em</TableCell>
              <TableCell>Expira em</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Motivo</TableCell>
              <TableCell>Trace ID</TableCell>
              <TableCell>Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sessions.map((session) => (
              <TableRow key={session.id} data-testid={`session-row-${session.id}`}>
                <TableCell>{session.userId}</TableCell>
                <TableCell>{session.issuedAt ? new Date(session.issuedAt).toLocaleString('pt-BR') : '—'}</TableCell>
                <TableCell>{session.expiresAt ? new Date(session.expiresAt).toLocaleString('pt-BR') : '—'}</TableCell>
                <TableCell>
                  <Chip
                    label={session.active && !session.revokedAt ? 'Ativa' : 'Revogada'}
                    color={session.active && !session.revokedAt ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>{session.revocationReason ?? '—'}</TableCell>
                <TableCell>
                  <Typography variant="caption" fontFamily="monospace">
                    {session.traceId}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Button
                    size="small"
                    color="error"
                    disabled={!session.active || !!session.revokedAt}
                    onClick={() => handleRevoke(session.id)}
                  >
                    Revogar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
