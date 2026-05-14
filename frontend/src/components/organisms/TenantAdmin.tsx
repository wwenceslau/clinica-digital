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
import InputAdornment from '@mui/material/InputAdornment';
import MenuItem from '@mui/material/MenuItem';
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
import { Add as AddIcon, AddCircleOutline as AddCircleOutlineIcon, Delete as DeleteIcon, Edit as EditIcon, Refresh as RefreshIcon, RemoveCircleOutline as RemoveCircleOutlineIcon } from '@mui/icons-material';
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
  orgFhirTypeJson?: string | null;
  orgFhirAliasJson?: string | null;
  orgFhirTelecomJson?: string | null;
  orgFhirAddressJson?: string | null;
  orgFhirPartOfOrgId?: string | null;
  orgFhirEndpointRefsJson?: string | null;
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

// ── FHIR value types ──────────────────────────────────────────────────────────

type TelecomSystem = 'phone' | 'fax' | 'email' | 'pager' | 'url' | 'sms' | 'other';
type TelecomUse = 'work' | 'home' | 'mobile' | 'old' | 'temp';

interface TelecomEntry {
  system: TelecomSystem;
  value: string;
  use: TelecomUse;
}

interface AddressEntry {
  use: 'work' | 'home' | 'billing' | 'old' | 'temp';
  line: string;        // logradouro + número
  complement: string;  // complemento / bairro
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

const EMPTY_TELECOM: TelecomEntry = { system: 'phone', value: '', use: 'work' };
const EMPTY_ADDRESS: AddressEntry = { use: 'work', line: '', complement: '', city: '', state: '', postalCode: '', country: 'BR' };

// ── FHIR Organization type options ────────────────────────────────────────────
const ORG_TYPE_OPTIONS = [
  { value: 'prov', label: 'Prestador de saúde (prov)' },
  { value: 'dept', label: 'Departamento hospitalar (dept)' },
  { value: 'team', label: 'Equipe de cuidados (team)' },
  { value: 'govt', label: 'Governo (govt)' },
  { value: 'ins', label: 'Seguradora (ins)' },
  { value: 'pay', label: 'Pagador (pay)' },
  { value: 'edu', label: 'Educacional (edu)' },
  { value: 'reli', label: 'Religioso (reli)' },
  { value: 'crs', label: 'Serviço de responsabilidade (crs)' },
  { value: 'cg', label: 'Grupo comunitário (cg)' },
  { value: 'bus', label: 'Negócios/Empresa (bus)' },
  { value: 'other', label: 'Outro (other)' },
];

// ── Form state ────────────────────────────────────────────────────────────────

interface FormState {
  tenantId: string;
  // Organization — required
  organizationDisplayName: string;
  cnes: string;
  // Organization — optional FHIR
  orgType: string;          // single FHIR type code
  orgAliases: string[];     // list of alternative names
  orgTelecom: TelecomEntry[];
  orgAddress: AddressEntry;
  orgPartOfOrgId: string;   // UUID of parent org
  // Admin — required
  adminDisplayName: string;
  adminEmail: string;
  adminCpf: string;
  adminPassword: string;
}

const EMPTY_FORM: FormState = {
  tenantId: '',
  organizationDisplayName: '',
  cnes: '',
  orgType: '',
  orgAliases: [],
  orgTelecom: [],
  orgAddress: { ...EMPTY_ADDRESS },
  orgPartOfOrgId: '',
  adminDisplayName: '',
  adminEmail: '',
  adminCpf: '',
  adminPassword: '',
};

// ── JSON serializers ──────────────────────────────────────────────────────────

function buildOrgTypeJson(typeCode: string): string | undefined {
  if (!typeCode) return undefined;
  return JSON.stringify([{
    coding: [{ system: 'http://terminology.hl7.org/CodeSystem/organization-type', code: typeCode }],
  }]);
}

function buildAliasJson(aliases: string[]): string | undefined {
  const cleaned = aliases.map((a) => a.trim()).filter(Boolean);
  return cleaned.length > 0 ? JSON.stringify(cleaned) : undefined;
}

function buildTelecomJson(entries: TelecomEntry[]): string | undefined {
  const valid = entries.filter((e) => e.value.trim());
  return valid.length > 0
    ? JSON.stringify(valid.map((e) => ({ system: e.system, value: e.value.trim(), use: e.use })))
    : undefined;
}

function buildAddressJson(addr: AddressEntry): string | undefined {
  if (!addr.line.trim() && !addr.city.trim()) return undefined;
  const lines = [addr.line.trim(), addr.complement.trim()].filter(Boolean);
  return JSON.stringify([{
    use: addr.use,
    line: lines,
    city: addr.city.trim(),
    state: addr.state.trim(),
    postalCode: addr.postalCode.trim(),
    country: addr.country.trim() || 'BR',
  }]);
}

// ── Parsers (JSON stored in DB back to structured) ────────────────────────────

function parseOrgType(json: string | null | undefined): string {
  try {
    const arr = JSON.parse(json ?? '[]') as Array<{ coding?: Array<{ code?: string }> }>;
    return arr[0]?.coding?.[0]?.code ?? '';
  } catch { return ''; }
}

function parseAliases(json: string | null | undefined): string[] {
  try { return (JSON.parse(json ?? '[]') as string[]).filter(Boolean); }
  catch { return []; }
}

function parseTelecom(json: string | null | undefined): TelecomEntry[] {
  try {
    const arr = JSON.parse(json ?? '[]') as TelecomEntry[];
    return arr.length > 0 ? arr : [];
  } catch { return []; }
}

function parseAddress(json: string | null | undefined): AddressEntry {
  try {
    const arr = JSON.parse(json ?? '[]') as Array<AddressEntry & { line?: string[] }>;
    const a = arr[0];
    if (!a) return { ...EMPTY_ADDRESS };
    return {
      use: (a.use as AddressEntry['use']) ?? 'work',
      line: Array.isArray(a.line) ? a.line[0] ?? '' : (a.line ?? ''),
      complement: Array.isArray((a as { line?: string[] }).line) ? (a as { line?: string[] }).line?.[1] ?? '' : '',
      city: a.city ?? '',
      state: a.state ?? '',
      postalCode: a.postalCode ?? '',
      country: a.country ?? 'BR',
    };
  } catch { return { ...EMPTY_ADDRESS }; }
}

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

  const canSubmit = !loading && (
    isEdit
      ? form.organizationDisplayName.trim().length > 0 && cnesValid && canPersistEdit
      : form.organizationDisplayName.trim().length > 0 &&
        cnesValid &&
        form.adminDisplayName.trim().length > 0 &&
        form.adminEmail.trim().length > 0 &&
        cpfValid &&
        form.adminPassword.trim().length > 0
  );

  const emptyStateText = useMemo(() => t('tenantAdmin.grid.empty'), [t]);

  function handleChange(field: keyof Pick<FormState, 'tenantId' | 'organizationDisplayName' | 'cnes' | 'orgType' | 'orgPartOfOrgId' | 'adminDisplayName' | 'adminEmail' | 'adminCpf' | 'adminPassword'>) {
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
        fhirTypeJson: buildOrgTypeJson(form.orgType),
        fhirAliasJson: buildAliasJson(form.orgAliases),
        fhirTelecomJson: buildTelecomJson(form.orgTelecom),
        fhirAddressJson: buildAddressJson(form.orgAddress),
        fhirPartOfOrgId: form.orgPartOfOrgId.trim() || undefined,
        fhirEndpointRefsJson: undefined,
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
      orgType: parseOrgType(tenant.orgFhirTypeJson),
      orgAliases: parseAliases(tenant.orgFhirAliasJson),
      orgTelecom: parseTelecom(tenant.orgFhirTelecomJson),
      orgAddress: parseAddress(tenant.orgFhirAddressJson),
      orgPartOfOrgId: tenant.orgFhirPartOfOrgId ?? '',
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

            {/* ── Dados da Organização ───────────────────────────────────── */}
            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5, textTransform: 'uppercase', letterSpacing: 0.8, fontSize: '0.72rem' }}>
              {t('tenantAdmin.modal.organizationSection')}
            </Typography>
            <Stack spacing={2} sx={{ mb: 2 }}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.organizationDisplayName')}
                  value={form.organizationDisplayName}
                  onChange={handleChange('organizationDisplayName')}
                  size="small"
                  required
                  fullWidth
                />
                <TextField
                  label={t('tenantAdmin.modal.cnes')}
                  value={form.cnes}
                  onChange={handleChange('cnes')}
                  size="small"
                  required
                  inputProps={{ maxLength: 7 }}
                  error={form.cnes.length > 0 && !cnesValid}
                  helperText={form.cnes.length > 0 && !cnesValid ? t('tenantAdmin.validation.cnes') : ''}
                  sx={{ minWidth: 180 }}
                />
              </Stack>

              {/* Tipo da organização */}
              <TextField
                select
                label="Tipo de Organização"
                value={form.orgType}
                onChange={handleChange('orgType')}
                size="small"
                fullWidth
                helperText="Opcional — classifica a organização no padrão FHIR R4"
              >
                <MenuItem value="">— Não especificado —</MenuItem>
                {ORG_TYPE_OPTIONS.map((opt) => (
                  <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
                ))}
              </TextField>

              {/* Nomes alternativos (aliases) */}
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                  Nomes alternativos / siglas — opcional
                </Typography>
                <Stack spacing={1}>
                  {form.orgAliases.map((alias, idx) => (
                    <Stack key={idx} direction="row" spacing={1} alignItems="center">
                      <TextField
                        value={alias}
                        onChange={(e) => {
                          const updated = [...form.orgAliases];
                          updated[idx] = e.target.value;
                          setForm((prev) => ({ ...prev, orgAliases: updated }));
                        }}
                        size="small"
                        fullWidth
                        placeholder={`Nome alternativo ${idx + 1}`}
                      />
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => setForm((prev) => ({ ...prev, orgAliases: prev.orgAliases.filter((_, i) => i !== idx) }))}
                        aria-label="Remover nome alternativo"
                      >
                        <RemoveCircleOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                  <Button
                    size="small"
                    startIcon={<AddCircleOutlineIcon />}
                    onClick={() => setForm((prev) => ({ ...prev, orgAliases: [...prev.orgAliases, ''] }))}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Adicionar nome alternativo
                  </Button>
                </Stack>
              </Box>
            </Stack>

            {/* ── Contato da Organização ─────────────────────────────────── */}
            <Divider sx={{ my: 2 }}>
              <Typography variant="caption" color="text.secondary">Contato e Localização</Typography>
            </Divider>
            <Stack spacing={2} sx={{ mb: 2 }}>

              {/* Telefones/Contatos */}
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                  Contatos (telefone, e-mail, site) — opcional
                </Typography>
                <Stack spacing={1}>
                  {form.orgTelecom.map((tc, idx) => (
                    <Stack key={idx} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
                      <TextField
                        select
                        label="Tipo"
                        value={tc.system}
                        onChange={(e) => {
                          const updated = [...form.orgTelecom];
                          updated[idx] = { ...updated[idx], system: e.target.value as TelecomSystem };
                          setForm((prev) => ({ ...prev, orgTelecom: updated }));
                        }}
                        size="small"
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="phone">Telefone</MenuItem>
                        <MenuItem value="fax">Fax</MenuItem>
                        <MenuItem value="email">E-mail</MenuItem>
                        <MenuItem value="url">Site</MenuItem>
                        <MenuItem value="sms">SMS</MenuItem>
                        <MenuItem value="other">Outro</MenuItem>
                      </TextField>
                      <TextField
                        label="Valor"
                        value={tc.value}
                        onChange={(e) => {
                          const updated = [...form.orgTelecom];
                          updated[idx] = { ...updated[idx], value: e.target.value };
                          setForm((prev) => ({ ...prev, orgTelecom: updated }));
                        }}
                        size="small"
                        fullWidth
                        placeholder={tc.system === 'phone' ? '+55 11 3000-0000' : tc.system === 'email' ? 'contato@clinica.com.br' : ''}
                      />
                      <TextField
                        select
                        label="Uso"
                        value={tc.use}
                        onChange={(e) => {
                          const updated = [...form.orgTelecom];
                          updated[idx] = { ...updated[idx], use: e.target.value as TelecomUse };
                          setForm((prev) => ({ ...prev, orgTelecom: updated }));
                        }}
                        size="small"
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="work">Trabalho</MenuItem>
                        <MenuItem value="home">Residencial</MenuItem>
                        <MenuItem value="mobile">Celular</MenuItem>
                        <MenuItem value="temp">Temporário</MenuItem>
                        <MenuItem value="old">Antigo</MenuItem>
                      </TextField>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => setForm((prev) => ({ ...prev, orgTelecom: prev.orgTelecom.filter((_, i) => i !== idx) }))}
                        aria-label="Remover contato"
                        sx={{ mt: 0.5 }}
                      >
                        <RemoveCircleOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                  <Button
                    size="small"
                    startIcon={<AddCircleOutlineIcon />}
                    onClick={() => setForm((prev) => ({ ...prev, orgTelecom: [...prev.orgTelecom, { ...EMPTY_TELECOM }] }))}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Adicionar contato
                  </Button>
                </Stack>
              </Box>

              {/* Endereço */}
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                  Endereço — opcional
                </Typography>
                <Stack spacing={1.5}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                      label="Logradouro e número"
                      value={form.orgAddress.line}
                      onChange={(e) => setForm((prev) => ({ ...prev, orgAddress: { ...prev.orgAddress, line: e.target.value } }))}
                      size="small"
                      fullWidth
                      placeholder="Av. Paulista, 1000"
                    />
                    <TextField
                      label="Complemento / Bairro"
                      value={form.orgAddress.complement}
                      onChange={(e) => setForm((prev) => ({ ...prev, orgAddress: { ...prev.orgAddress, complement: e.target.value } }))}
                      size="small"
                      fullWidth
                      placeholder="Sala 101, Bela Vista"
                    />
                  </Stack>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                      label="Cidade"
                      value={form.orgAddress.city}
                      onChange={(e) => setForm((prev) => ({ ...prev, orgAddress: { ...prev.orgAddress, city: e.target.value } }))}
                      size="small"
                      fullWidth
                      placeholder="São Paulo"
                    />
                    <TextField
                      label="Estado (UF)"
                      value={form.orgAddress.state}
                      onChange={(e) => setForm((prev) => ({ ...prev, orgAddress: { ...prev.orgAddress, state: e.target.value } }))}
                      size="small"
                      inputProps={{ maxLength: 2 }}
                      placeholder="SP"
                      sx={{ minWidth: 100, maxWidth: 100 }}
                    />
                    <TextField
                      label="CEP"
                      value={form.orgAddress.postalCode}
                      onChange={(e) => setForm((prev) => ({ ...prev, orgAddress: { ...prev.orgAddress, postalCode: e.target.value } }))}
                      size="small"
                      inputProps={{ maxLength: 9 }}
                      placeholder="01310-100"
                      InputProps={{
                        endAdornment: <InputAdornment position="end">BR</InputAdornment>,
                      }}
                      sx={{ minWidth: 150 }}
                    />
                  </Stack>
                  <TextField
                    label="Organização superior (UUID) — opcional"
                    value={form.orgPartOfOrgId}
                    onChange={handleChange('orgPartOfOrgId')}
                    size="small"
                    fullWidth
                    placeholder="UUID da organização matriz, se houver"
                    helperText="Preencha apenas se esta organização for subordinada a outra"
                  />
                </Stack>
              </Box>
            </Stack>

            {/* ── Dados do Administrador ─────────────────────────────────── */}
            <Divider sx={{ my: 2 }}>
              <Typography variant="caption" color="text.secondary">
                {t('tenantAdmin.modal.adminSection')}
              </Typography>
            </Divider>
            <Stack spacing={2}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.adminDisplayName')}
                  value={form.adminDisplayName}
                  onChange={handleChange('adminDisplayName')}
                  size="small"
                  required
                  fullWidth
                />
                <TextField
                  label={t('tenantAdmin.modal.adminEmail')}
                  type="email"
                  value={form.adminEmail}
                  onChange={handleChange('adminEmail')}
                  size="small"
                  required
                  fullWidth
                />
              </Stack>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('tenantAdmin.modal.adminCpf')}
                  value={form.adminCpf}
                  onChange={handleChange('adminCpf')}
                  size="small"
                  required
                  inputProps={{ maxLength: 11 }}
                  error={form.adminCpf.length > 0 && !cpfValid}
                  helperText={form.adminCpf.length > 0 && !cpfValid ? t('tenantAdmin.validation.cpf') : ''}
                  sx={{ minWidth: 200 }}
                />
                <TextField
                  label={t('tenantAdmin.modal.adminPassword')}
                  type="password"
                  value={form.adminPassword}
                  onChange={handleChange('adminPassword')}
                  size="small"
                  required
                  fullWidth
                  helperText={isEdit ? 'Deixe em branco para manter a senha atual' : ''}
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

