/**
 * T136 [US2] TenantAdmin organism — corrected form with all required fields.
 *
 * Displays the tenant list and allows super-user to create new tenants via
 * AdminTenantController (POST /api/admin/tenants).
 *
 * Required fields: organizationDisplayName, cnes (7 digits), adminDisplayName,
 * adminEmail, adminCpf (11 digits), adminPassword.
 *
 * Refs: FR-003, FR-022
 */

import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import type { CreateTenantRequest } from '../../services/tenantApi';

export interface Tenant {
  id: string;
  legalName: string;
  slug: string;
  status: 'active' | 'suspended' | 'blocked';
  planTier: string;
  adminDisplayName?: string;
  adminEmail?: string;
  adminCpf?: string;
}

export interface TenantAdminProps {
  tenants: Tenant[];
  onCreateTenant: (payload: CreateTenantRequest) => void | Promise<void>;
  onUpdateTenant?: (tenantId: string, payload: CreateTenantRequest) => void | Promise<void>;
  onDeleteTenant?: (tenantId: string) => void | Promise<void>;
  onRefresh?: () => void | Promise<void>;
  loading?: boolean;
}

const STATUS_COLORS: Record<Tenant['status'], 'success' | 'warning' | 'error'> = {
  active: 'success',
  suspended: 'warning',
  blocked: 'error',
};

const STATUS_LABELS: Record<Tenant['status'], string> = {
  active: 'Ativo',
  suspended: 'Suspenso',
  blocked: 'Bloqueado',
};

interface FormState {
  tenantId: string;
  organizationDisplayName: string;
  cnes: string;
  adminDisplayName: string;
  adminEmail: string;
  adminCpf: string;
  adminPassword: string;
}

const EMPTY_FORM: FormState = {
  tenantId: '',
  organizationDisplayName: '',
  cnes: '',
  adminDisplayName: '',
  adminEmail: '',
  adminCpf: '',
  adminPassword: '',
};

export function TenantAdmin({
  tenants,
  onCreateTenant,
  onUpdateTenant,
  onDeleteTenant,
  onRefresh,
  loading = false,
}: TenantAdminProps) {
  const { t } = useTranslation();
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<'create' | 'edit'>('create');

  const cnesValid = /^\d{7}$/.test(form.cnes);
  const cpfValid = /^\d{11}$/.test(form.adminCpf);
  const isEdit = mode === 'edit';
  const canPersistEdit = Boolean(onUpdateTenant);
  const canDelete = Boolean(onDeleteTenant);
  const modalTitle = isEdit ? t('tenantAdmin.modal.editTitle') : t('tenantAdmin.modal.createTitle');

  const canSubmit =
    form.organizationDisplayName.trim().length > 0 &&
    cnesValid &&
    form.adminDisplayName.trim().length > 0 &&
    form.adminEmail.trim().length > 0 &&
    cpfValid &&
    form.adminPassword.trim().length > 0 &&
    !loading &&
    (!isEdit || canPersistEdit);

  const emptyStateText = useMemo(() => t('tenantAdmin.grid.empty'), [t]);

  function handleChange(field: keyof FormState) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
    };
  }

  function handleCreate(event?: React.FormEvent) {
    event?.preventDefault();
    if (!canSubmit) return;
    const payload: CreateTenantRequest = {
      organization: {
        displayName: form.organizationDisplayName.trim(),
        cnes: form.cnes.trim(),
      },
      adminPractitioner: {
        displayName: form.adminDisplayName.trim(),
        email: form.adminEmail.trim(),
        cpf: form.adminCpf.trim(),
        password: form.adminPassword,
      },
    };

    if (isEdit && form.tenantId && onUpdateTenant) {
      onUpdateTenant(form.tenantId, payload);
    } else if (!isEdit) {
      onCreateTenant(payload);
    }

    setForm(EMPTY_FORM);
    setModalOpen(false);
    setMode('create');
  }

  function openCreateModal() {
    setMode('create');
    setForm(EMPTY_FORM);
    setModalOpen(true);
  }

  function openEditModal(tenant: Tenant) {
    setMode('edit');
    setForm({
      tenantId: tenant.id,
      organizationDisplayName: tenant.legalName,
      cnes: tenant.slug,
      adminDisplayName: tenant.adminDisplayName ?? '',
      adminEmail: tenant.adminEmail ?? '',
      adminCpf: tenant.adminCpf ?? '',
      adminPassword: '',
    });
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setMode('create');
    setForm(EMPTY_FORM);
  }

  function handleDelete(tenantId: string) {
    if (!onDeleteTenant) return;
    if (!window.confirm(t('tenantAdmin.actions.deleteConfirm'))) return;
    onDeleteTenant(tenantId);
  }

  return (
    <Box data-testid="tenant-admin">
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">{t('tenantAdmin.title')}</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title={t('tenantAdmin.actions.refresh')}>
            <span>
              <IconButton onClick={onRefresh} disabled={loading || !onRefresh} aria-label={t('tenantAdmin.actions.refresh')}>
                <RefreshIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateModal} data-testid="btn-criar-tenant">
            {t('tenantAdmin.actions.new')}
          </Button>
        </Box>
      </Box>

      <TableContainer component={Paper} sx={{ mt: 3 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('tenantAdmin.grid.legalName')}</TableCell>
              <TableCell>{t('tenantAdmin.grid.slug')}</TableCell>
              <TableCell>{t('tenantAdmin.grid.status')}</TableCell>
              <TableCell>{t('tenantAdmin.grid.plan')}</TableCell>
              <TableCell align="right">{t('tenantAdmin.grid.actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {tenants.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                  {emptyStateText}
                </TableCell>
              </TableRow>
            ) : (
              tenants.map((tenant) => (
                <TableRow key={tenant.id} data-testid={`tenant-row-${tenant.id}`}>
                  <TableCell>{tenant.legalName}</TableCell>
                  <TableCell>
                    <Typography variant="caption" fontFamily="monospace">
                      {tenant.slug}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={STATUS_LABELS[tenant.status]}
                      color={STATUS_COLORS[tenant.status]}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{tenant.planTier}</TableCell>
                  <TableCell align="right">
                    <Tooltip title={t('tenantAdmin.actions.edit')}>
                      <span>
                        <IconButton
                          size="small"
                          onClick={() => openEditModal(tenant)}
                          data-testid={`tenant-edit-${tenant.id}`}
                          disabled={!canPersistEdit}
                          aria-label={t('tenantAdmin.actions.edit')}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title={t('tenantAdmin.actions.delete')}>
                      <span>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDelete(tenant.id)}
                          data-testid={`tenant-delete-${tenant.id}`}
                          disabled={!canDelete}
                          aria-label={t('tenantAdmin.actions.delete')}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={modalOpen} onClose={closeModal} fullWidth maxWidth="md">
        <DialogTitle>{modalTitle}</DialogTitle>
        <DialogContent>
          <Box component="form" onSubmit={handleCreate} data-testid="tenant-create-form" sx={{ mt: 1 }}>
            <Typography variant="subtitle2" gutterBottom>
              {t('tenantAdmin.modal.organizationSection')}
            </Typography>
            <Stack spacing={2} sx={{ mb: 1 }}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.organizationDisplayName')}
                  name="organizationDisplayName"
                  value={form.organizationDisplayName}
                  onChange={handleChange('organizationDisplayName')}
                  size="small"
                  required
                  fullWidth
                  inputProps={{ 'aria-label': t('tenantAdmin.modal.organizationDisplayName') }}
                />
                <TextField
                  label={t('tenantAdmin.modal.cnes')}
                  name="cnes"
                  value={form.cnes}
                  onChange={handleChange('cnes')}
                  size="small"
                  required
                  inputProps={{ maxLength: 7, 'aria-label': t('tenantAdmin.modal.cnes') }}
                  error={form.cnes.length > 0 && !cnesValid}
                  helperText={form.cnes.length > 0 && !cnesValid ? t('tenantAdmin.validation.cnes') : ''}
                  sx={{ minWidth: 180 }}
                />
              </Stack>

              <Divider>
                <Typography variant="caption" color="text.secondary">
                  {t('tenantAdmin.modal.adminSection')}
                </Typography>
              </Divider>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.adminDisplayName')}
                  name="adminDisplayName"
                  value={form.adminDisplayName}
                  onChange={handleChange('adminDisplayName')}
                  size="small"
                  required
                  fullWidth
                  inputProps={{ 'aria-label': t('tenantAdmin.modal.adminDisplayName') }}
                />
                <TextField
                  label={t('tenantAdmin.modal.adminEmail')}
                  name="adminEmail"
                  type="email"
                  value={form.adminEmail}
                  onChange={handleChange('adminEmail')}
                  size="small"
                  required
                  fullWidth
                  inputProps={{ 'aria-label': t('tenantAdmin.modal.adminEmail') }}
                />
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.adminCpf')}
                  name="adminCpf"
                  value={form.adminCpf}
                  onChange={handleChange('adminCpf')}
                  size="small"
                  required
                  inputProps={{ maxLength: 11, 'aria-label': t('tenantAdmin.modal.adminCpf') }}
                  error={form.adminCpf.length > 0 && !cpfValid}
                  helperText={form.adminCpf.length > 0 && !cpfValid ? t('tenantAdmin.validation.cpf') : ''}
                  sx={{ minWidth: 200 }}
                />
                <TextField
                  label={t('tenantAdmin.modal.adminPassword')}
                  name="adminPassword"
                  type="password"
                  value={form.adminPassword}
                  onChange={handleChange('adminPassword')}
                  size="small"
                  required
                  fullWidth
                  inputProps={{ 'aria-label': t('tenantAdmin.modal.adminPassword') }}
                />
              </Stack>
            </Stack>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeModal}>{t('tenantAdmin.actions.cancel')}</Button>
          <Button onClick={() => handleCreate()} type="button" variant="contained" disabled={!canSubmit}>
            {loading
              ? t('tenantAdmin.actions.saving')
              : isEdit
                ? t('tenantAdmin.actions.save')
                : t('tenantAdmin.actions.create')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

