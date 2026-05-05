/**
 * SecurityRolesPage — /admin/security/roles
 *
 * Manages RBAC groups within the tenant: list groups, create group,
 * view group permissions, and add permissions.
 *
 * Uses iamGroupApi.ts to call AdminGroupController endpoints.
 *
 * Refs: T104, FR-006
 */
import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Paper,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Refresh as RefreshIcon, ExpandMore as ExpandMoreIcon, ExpandLess as ExpandLessIcon, Security as SecurityIcon } from '@mui/icons-material';
import {
  listGroups,
  createGroup,
  listGroupPermissions,
  listPermissions,
  assignPermissionToGroup,
  type IamGroup,
  type IamPermission,
} from '../services/iamGroupApi';
import { useAuth } from '../context/AuthContext';

export function SecurityRolesPage(): React.ReactElement {
  const { session } = useAuth();
  const sessionId = session?.sessionId ?? '';
  const tenantId = session?.tenant?.id ?? '';

  const [groups, setGroups] = useState<IamGroup[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedGroup, setExpandedGroup] = useState<string | null>(null);
  const [groupPermissions, setGroupPermissions] = useState<Record<string, IamPermission[]>>({});
  const [allPermissions, setAllPermissions] = useState<IamPermission[]>([]);

  // Create group dialog
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Add permission dialog
  const [addPermOpen, setAddPermOpen] = useState(false);
  const [addPermGroupId, setAddPermGroupId] = useState<string>('');
  const [addPermCode, setAddPermCode] = useState('');
  const [addingPerm, setAddingPerm] = useState(false);
  const [addPermError, setAddPermError] = useState<string | null>(null);

  const loadGroups = useCallback(() => {
    if (!sessionId || !tenantId) return;
    setLoading(true);
    setError(null);
    listGroups(tenantId, sessionId)
      .then(setGroups)
      .catch(() => setError('Erro ao carregar grupos de permissão.'))
      .finally(() => setLoading(false));
  }, [sessionId, tenantId]);

  const loadAllPermissions = useCallback(() => {
    if (!sessionId || !tenantId) return;
    listPermissions(tenantId, sessionId)
      .then(setAllPermissions)
      .catch(() => {}); // non-critical
  }, [sessionId, tenantId]);

  useEffect(() => {
    loadGroups();
    loadAllPermissions();
  }, [loadGroups, loadAllPermissions]);

  function toggleGroup(groupId: string) {
    if (expandedGroup === groupId) {
      setExpandedGroup(null);
      return;
    }
    setExpandedGroup(groupId);
    if (!groupPermissions[groupId]) {
      listGroupPermissions(tenantId, sessionId, groupId)
        .then((perms) => setGroupPermissions((prev) => ({ ...prev, [groupId]: perms })))
        .catch(() => setGroupPermissions((prev) => ({ ...prev, [groupId]: [] })));
    }
  }

  async function handleCreateGroup(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    setCreateError(null);
    try {
      const g = await createGroup(tenantId, sessionId, createName, createDesc);
      setGroups((prev) => [...prev, g]);
      setCreateName('');
      setCreateDesc('');
      setCreateOpen(false);
    } catch {
      setCreateError('Erro ao criar grupo. Verifique se o nome já existe.');
    } finally {
      setCreating(false);
    }
  }

  function openAddPerm(groupId: string) {
    setAddPermGroupId(groupId);
    setAddPermCode('');
    setAddPermError(null);
    setAddPermOpen(true);
  }

  async function handleAddPermission(e: React.FormEvent) {
    e.preventDefault();
    setAddingPerm(true);
    setAddPermError(null);
    try {
      await assignPermissionToGroup(tenantId, sessionId, addPermGroupId, addPermCode);
      // Refresh permissions for the group
      const updated = await listGroupPermissions(tenantId, sessionId, addPermGroupId);
      setGroupPermissions((prev) => ({ ...prev, [addPermGroupId]: updated }));
      setAddPermOpen(false);
    } catch {
      setAddPermError('Erro ao adicionar permissão.');
    } finally {
      setAddingPerm(false);
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5" component="h1">
          Perfis de Acesso (RBAC)
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Atualizar">
            <IconButton onClick={loadGroups} disabled={loading} aria-label="atualizar">
              {loading ? <CircularProgress size={20} /> : <RefreshIcon />}
            </IconButton>
          </Tooltip>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateOpen(true)}
          >
            Novo Grupo
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {groups.length === 0 && !loading ? (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          <SecurityIcon sx={{ fontSize: 48, mb: 1, opacity: 0.4 }} />
          <Typography>Nenhum grupo de permissão cadastrado.</Typography>
          <Button variant="outlined" sx={{ mt: 2 }} onClick={() => setCreateOpen(true)}>
            Criar primeiro grupo
          </Button>
        </Paper>
      ) : (
        <Paper variant="outlined">
          <List disablePadding>
            {groups.map((group, idx) => (
              <React.Fragment key={group.groupId}>
                {idx > 0 && <Divider />}
                <ListItemButton onClick={() => toggleGroup(group.groupId)}>
                  <ListItemText
                    primary={group.name}
                    secondary={group.description || undefined}
                  />
                  <Button
                    size="small"
                    variant="outlined"
                    sx={{ mr: 1 }}
                    onClick={(e) => { e.stopPropagation(); openAddPerm(group.groupId); }}
                  >
                    + Permissão
                  </Button>
                  {expandedGroup === group.groupId ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                </ListItemButton>
                <Collapse in={expandedGroup === group.groupId} timeout="auto" unmountOnExit>
                  <Box sx={{ pl: 3, pr: 2, pb: 2, pt: 1, bgcolor: 'grey.50' }}>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                      Permissões concedidas:
                    </Typography>
                    {(groupPermissions[group.groupId] ?? []).length === 0 ? (
                      <Typography variant="body2" color="text.secondary">
                        Nenhuma permissão atribuída.
                      </Typography>
                    ) : (
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                        {(groupPermissions[group.groupId] ?? []).map((p) => (
                          <Chip
                            key={p.permissionId}
                            label={p.code}
                            size="small"
                            variant="outlined"
                            color="primary"
                            title={p.description ?? p.code}
                          />
                        ))}
                      </Box>
                    )}
                  </Box>
                </Collapse>
              </React.Fragment>
            ))}
          </List>
        </Paper>
      )}

      {/* ── Create group dialog ───────────────────────────────────── */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Novo Grupo de Permissão</DialogTitle>
        <form onSubmit={handleCreateGroup}>
          <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {createError && <Alert severity="error">{createError}</Alert>}
            <TextField
              label="Nome do grupo"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              required
              autoFocus
              fullWidth
              inputProps={{ maxLength: 100 }}
            />
            <TextField
              label="Descrição (opcional)"
              value={createDesc}
              onChange={(e) => setCreateDesc(e.target.value)}
              fullWidth
              multiline
              rows={2}
              inputProps={{ maxLength: 500 }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)} disabled={creating}>Cancelar</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={creating || !createName.trim()}
              startIcon={creating ? <CircularProgress size={16} /> : null}
            >
              {creating ? 'Criando…' : 'Criar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* ── Add permission dialog ─────────────────────────────────── */}
      <Dialog open={addPermOpen} onClose={() => setAddPermOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Adicionar Permissão ao Grupo</DialogTitle>
        <form onSubmit={handleAddPermission}>
          <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {addPermError && <Alert severity="error">{addPermError}</Alert>}
            <TextField
              label="Código da permissão"
              value={addPermCode}
              onChange={(e) => setAddPermCode(e.target.value)}
              required
              autoFocus
              fullWidth
              helperText={
                allPermissions.length > 0
                  ? `Disponíveis: ${allPermissions.map((p) => p.code).join(', ')}`
                  : 'Ex: tenants:read, users:write'
              }
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAddPermOpen(false)} disabled={addingPerm}>Cancelar</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={addingPerm || !addPermCode.trim()}
              startIcon={addingPerm ? <CircularProgress size={16} /> : null}
            >
              {addingPerm ? 'Adicionando…' : 'Adicionar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
