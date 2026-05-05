/**
 * T058 [US4] SessionHistory organism.
 *
 * Displays a tabular audit trail of recent IAM session events.
 *
 * Refs: FR-004
 */

import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';

export interface SessionItem {
  id: string;
  tenantSlug: string;
  user: string;
  outcome: 'success' | 'failure';
  traceId: string;
}

export interface SessionHistoryProps {
  sessions: SessionItem[];
}

export function SessionHistory({ sessions }: SessionHistoryProps) {
  return (
    <Box data-testid="session-history">
      <Typography variant="h6" gutterBottom>
        Histórico de sessões
      </Typography>
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Tenant</TableCell>
              <TableCell>Usuário</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Trace ID</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sessions.map((session) => (
              <TableRow key={session.id} data-testid={`session-row-${session.id}`}>
                <TableCell>{session.tenantSlug}</TableCell>
                <TableCell>{session.user}</TableCell>
                <TableCell>
                  <Chip
                    label={session.outcome === 'success' ? 'Sucesso' : 'Falha'}
                    color={session.outcome === 'success' ? 'success' : 'error'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Typography variant="caption" fontFamily="monospace">
                    {session.traceId}
                  </Typography>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
