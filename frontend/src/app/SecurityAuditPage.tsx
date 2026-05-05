/**
 * SecurityAuditPage — /admin/security/audit
 *
 * Displays the tenant's IAM audit trail from GET /api/admin/audit.
 *
 * Refs: T070, FR-016, FR-024
 */
import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import { Refresh as RefreshIcon } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';

interface AuditEventSummary {
  id: string;
  actorUserId: string | null;
  eventType: string;
  outcome: string;
  traceId: string | null;
  createdAt: string | null;
}

const OUTCOME_COLOR: Record<string, 'success' | 'error' | 'warning' | 'default'> = {
  success: 'success',
  SUCCESS: 'success',
  failure: 'error',
  FAILURE: 'error',
  error: 'error',
  ERROR: 'error',
};

async function fetchAuditEvents(
  sessionId: string,
  tenantId: string,
  limit: number,
): Promise<AuditEventSummary[]> {
  const res = await fetch(`/api/admin/audit?limit=${limit}`, {
    headers: {
      'X-Tenant-ID': tenantId,
      Authorization: `Bearer ${sessionId}`,
    },
  });
  if (!res.ok) throw new Error(`Erro ao carregar auditoria: ${res.status}`);
  return res.json();
}

export function SecurityAuditPage(): React.ReactElement {
  const { session } = useAuth();
  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const [events, setEvents] = useState<AuditEventSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [limit, setLimit] = useState(50);

  const load = useCallback(() => {
    if (!sessionId || !tenantId) return;
    setLoading(true);
    setError(null);
    fetchAuditEvents(sessionId, tenantId, limit)
      .then(setEvents)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [sessionId, tenantId, limit]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          Trilha de Auditoria
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 100 }}>
            <InputLabel id="limit-label">Exibir</InputLabel>
            <Select
              labelId="limit-label"
              value={limit}
              label="Exibir"
              onChange={(e) => setLimit(Number(e.target.value))}
            >
              <MenuItem value={25}>25</MenuItem>
              <MenuItem value={50}>50</MenuItem>
              <MenuItem value={100}>100</MenuItem>
              <MenuItem value={200}>200</MenuItem>
            </Select>
          </FormControl>
          <Tooltip title="Atualizar">
            <IconButton onClick={load} disabled={loading} aria-label="atualizar auditoria">
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label="trilha de auditoria">
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.50' }}>
              <TableCell><strong>Data/Hora</strong></TableCell>
              <TableCell><strong>Evento</strong></TableCell>
              <TableCell><strong>Resultado</strong></TableCell>
              <TableCell><strong>Ator (userId)</strong></TableCell>
              <TableCell><strong>Trace ID</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {events.length === 0 && !loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Nenhum evento de auditoria encontrado.
                </TableCell>
              </TableRow>
            ) : (
              events.map((e) => (
                <TableRow key={e.id} hover>
                  <TableCell sx={{ whiteSpace: 'nowrap', fontSize: 12 }}>
                    {e.createdAt
                      ? new Date(e.createdAt).toLocaleString('pt-BR')
                      : '—'}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace" fontSize={12}>
                      {e.eventType}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={e.outcome}
                      size="small"
                      color={OUTCOME_COLOR[e.outcome] ?? 'default'}
                    />
                  </TableCell>
                  <TableCell sx={{ fontSize: 11, color: 'text.secondary', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.actorUserId ?? '—'}
                  </TableCell>
                  <TableCell sx={{ fontSize: 11, color: 'text.secondary', maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.traceId ?? '—'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
