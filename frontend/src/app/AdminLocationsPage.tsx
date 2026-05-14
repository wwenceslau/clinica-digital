import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Switch,
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
  Add as AddIcon,
  AddCircleOutline as AddCircleOutlineIcon,
  Block as BlockIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
  RemoveCircleOutline as RemoveCircleOutlineIcon,
} from '@mui/icons-material';
import { useTranslation } from 'react-i18next';
import { useTenant } from '../context/TenantContext';
import { fromCaughtError } from '../services/operationOutcomeAdapter';
import {
  createAdminLocation,
  deactivateAdminLocation,
  listAdminLocations,
  type AdminLocation,
  updateAdminLocation,
} from '../services/adminLocationApi';

// ── FHIR Location value types ─────────────────────────────────────────────────
type LocationStatus = 'active' | 'suspended' | 'inactive';
type LocationMode = 'instance' | 'kind';
type TelecomSystem = 'phone' | 'fax' | 'email' | 'url' | 'sms' | 'other';
type TelecomUse = 'work' | 'home' | 'mobile' | 'old' | 'temp';

interface TelecomEntry {
  system: TelecomSystem;
  value: string;
  use: TelecomUse;
}

interface AddressEntry {
  line: string;
  complement: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

const EMPTY_TELECOM: TelecomEntry = { system: 'phone', value: '', use: 'work' };
const EMPTY_ADDRESS: AddressEntry = { line: '', complement: '', city: '', state: '', postalCode: '', country: 'BR' };

// ── JSON serializers ──────────────────────────────────────────────────────────
function buildTelecomJson(entries: TelecomEntry[]): string | undefined {
  const valid = entries.filter((e) => e.value.trim());
  return valid.length > 0
    ? JSON.stringify(valid.map((e) => ({ system: e.system, value: e.value.trim(), use: e.use })))
    : undefined;
}

function buildAddressJson(addr: AddressEntry): string | undefined {
  if (!addr.line.trim() && !addr.city.trim()) return undefined;
  const lines = [addr.line.trim(), addr.complement.trim()].filter(Boolean);
  return JSON.stringify({
    use: 'work',
    line: lines,
    city: addr.city.trim(),
    state: addr.state.trim(),
    postalCode: addr.postalCode.trim(),
    country: addr.country.trim() || 'BR',
  });
}

// ── Parsers ───────────────────────────────────────────────────────────────────
function parseTelecom(json: string | null | undefined): TelecomEntry[] {
  try {
    const arr = JSON.parse(json ?? '[]') as TelecomEntry[];
    return arr.length > 0 ? arr : [];
  } catch { return []; }
}

function parseAddress(json: string | null | undefined): AddressEntry {
  try {
    const raw = JSON.parse(json ?? 'null') as null | (AddressEntry & { line?: string[] | string });
    if (!raw) return { ...EMPTY_ADDRESS };
    const obj = Array.isArray(raw) ? (raw as (AddressEntry & { line?: string[] })[]) [0] : raw;
    if (!obj) return { ...EMPTY_ADDRESS };
    return {
      line: Array.isArray(obj.line) ? obj.line[0] ?? '' : (obj.line ?? ''),
      complement: Array.isArray(obj.line) ? obj.line[1] ?? '' : '',
      city: obj.city ?? '',
      state: obj.state ?? '',
      postalCode: obj.postalCode ?? '',
      country: obj.country ?? 'BR',
    };
  } catch { return { ...EMPTY_ADDRESS }; }
}

// ── Draft ─────────────────────────────────────────────────────────────────────
type LocationDraft = {
  displayName: string;
  name: string;
  status: LocationStatus;
  mode: LocationMode;
  accountActive: boolean;
  telecom: TelecomEntry[];
  address: AddressEntry;
};

const EMPTY_DRAFT: LocationDraft = {
  displayName: '',
  name: '',
  status: 'active',
  mode: 'instance',
  accountActive: true,
  telecom: [],
  address: { ...EMPTY_ADDRESS },
};

const STATUS_COLORS: Record<LocationStatus, 'success' | 'warning' | 'default'> = {
  active: 'success',
  suspended: 'warning',
  inactive: 'default',
};

export function AdminLocationsPage(): React.ReactElement {
  const { t } = useTranslation();
  const tenant = useTenant();
  const [rows, setRows] = useState<AdminLocation[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AdminLocation | null>(null);
  const [draft, setDraft] = useState<LocationDraft>(EMPTY_DRAFT);

  const organizationId = tenant.organizationId ?? tenant.tenantId ?? '';
  const tenantId = tenant.tenantId ?? '';

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => a.displayName.localeCompare(b.displayName, 'pt-BR')),
    [rows],
  );

  function openCreateDialog() {
    setEditing(null);
    setDraft(EMPTY_DRAFT);
    setDialogOpen(true);
  }

  function openEditDialog(item: AdminLocation) {
    setEditing(item);
    setDraft({
      displayName: item.displayName,
      name: item.fhirName,
      status: (item.fhirStatus as LocationStatus) ?? 'active',
      mode: (item.fhirMode as LocationMode) ?? 'instance',
      accountActive: item.accountActive,
      telecom: parseTelecom(item.fhirTelecomJson),
      address: parseAddress(item.fhirAddressJson),
    });
    setDialogOpen(true);
  }

  function closeDialog() {
    setDialogOpen(false);
    setEditing(null);
    setDraft(EMPTY_DRAFT);
  }

  function refresh() {
    setLoading(true);
    setErrorMessage(null);
    listAdminLocations(tenantId)
      .then((data) => setRows(data))
      .catch((err) => {
        const outcome = fromCaughtError(err);
        setErrorMessage(outcome.userMessage);
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    if (!tenantId) return;
    listAdminLocations(tenantId)
      .then((data) => setRows(data))
      .catch((err) => {
        const outcome = fromCaughtError(err);
        setErrorMessage(outcome.userMessage);
      })
      .finally(() => setLoading(false));
    setLoading(true);
    setErrorMessage(null);
  }, [tenantId]);

  async function handleSave() {
    if (!organizationId || !draft.displayName.trim()) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      if (editing) {
        const updated = await updateAdminLocation(tenantId, editing.id, {
          displayName: draft.displayName.trim(),
          fhirName: draft.name.trim() || draft.displayName.trim(),
          fhirStatus: draft.status,
          fhirMode: draft.mode,
          accountActive: draft.accountActive,
          fhirTelecomJson: buildTelecomJson(draft.telecom),
          fhirAddressJson: buildAddressJson(draft.address),
        });
        setRows((current) => current.map((r) => (r.id === updated.id ? updated : r)));
      } else {
        const created = await createAdminLocation(tenantId, {
          organizationId,
          displayName: draft.displayName.trim(),
          fhirName: draft.name.trim() || draft.displayName.trim(),
          fhirStatus: draft.status,
          fhirMode: draft.mode,
          fhirTelecomJson: buildTelecomJson(draft.telecom),
          fhirAddressJson: buildAddressJson(draft.address),
        });
        setRows((current) => [...current, created]);
      }
      closeDialog();
    } catch (err) {
      const outcome = fromCaughtError(err);
      setErrorMessage(outcome.userMessage);
    } finally {
      setLoading(false);
    }
  }

  async function handleDeactivate(item: AdminLocation) {
    setLoading(true);
    setErrorMessage(null);

    try {
      const updated = await deactivateAdminLocation(tenantId, item.id);
      setRows((current) => current.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      const outcome = fromCaughtError(err);
      setErrorMessage(outcome.userMessage);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">{t('adminLocations.title')}</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title={t('adminLocations.actions.refresh')}>
            <span>
              <IconButton onClick={refresh} disabled={loading} aria-label={t('adminLocations.actions.refresh')}>
                <RefreshIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog} disabled={loading || !organizationId}>
            {t('adminLocations.actions.new')}
          </Button>
        </Box>
      </Box>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label={t('adminLocations.grid.ariaLabel')}>
          <TableHead>
            <TableRow>
              <TableCell><strong>{t('adminLocations.grid.name')}</strong></TableCell>
              <TableCell><strong>{t('adminLocations.grid.status')}</strong></TableCell>
              <TableCell><strong>{t('adminLocations.grid.mode')}</strong></TableCell>
              <TableCell><strong>{t('adminLocations.grid.active')}</strong></TableCell>
              <TableCell><strong>{t('adminLocations.grid.actions')}</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {sortedRows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">{t('adminLocations.grid.empty')}</TableCell>
              </TableRow>
            ) : (
              sortedRows.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>{item.displayName}</TableCell>
                  <TableCell>
                    <Chip
                      label={t(`adminLocations.status.${item.fhirStatus}`)}
                      color={STATUS_COLORS[item.fhirStatus as LocationStatus] ?? 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{t(`adminLocations.mode.${item.fhirMode}`)}</TableCell>
                  <TableCell>{item.accountActive ? t('adminLocations.grid.yes') : t('adminLocations.grid.no')}</TableCell>
                  <TableCell>
                    <Tooltip title={t('adminLocations.actions.edit')}>
                      <IconButton size="small" onClick={() => openEditDialog(item)} aria-label={t('adminLocations.actions.edit')}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('adminLocations.actions.deactivate')}>
                      <span>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDeactivate(item)}
                          disabled={!item.accountActive || loading}
                          aria-label={t('adminLocations.actions.deactivate')}
                        >
                          <BlockIcon fontSize="small" />
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

      <Dialog open={dialogOpen} onClose={closeDialog} maxWidth="md" fullWidth>
        <DialogTitle>{editing ? t('adminLocations.modal.editTitle') : t('adminLocations.modal.createTitle')}</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {/* Identificação */}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label={t('adminLocations.modal.displayName')}
                value={draft.displayName}
                onChange={(e) => setDraft((prev) => ({ ...prev, displayName: e.target.value }))}
                required
                fullWidth
                size="small"
              />
              <TextField
                label={t('adminLocations.modal.name')}
                value={draft.name}
                onChange={(e) => setDraft((prev) => ({ ...prev, name: e.target.value }))}
                fullWidth
                size="small"
                helperText={t('adminLocations.modal.nameHelper')}
              />
            </Stack>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                select
                label={t('adminLocations.modal.status')}
                value={draft.status}
                onChange={(e) => setDraft((prev) => ({ ...prev, status: e.target.value as LocationStatus }))}
                size="small"
                fullWidth
              >
                <MenuItem value="active">{t('adminLocations.status.active')}</MenuItem>
                <MenuItem value="suspended">{t('adminLocations.status.suspended')}</MenuItem>
                <MenuItem value="inactive">{t('adminLocations.status.inactive')}</MenuItem>
              </TextField>
              <TextField
                select
                label={t('adminLocations.modal.mode')}
                value={draft.mode}
                onChange={(e) => setDraft((prev) => ({ ...prev, mode: e.target.value as LocationMode }))}
                size="small"
                fullWidth
              >
                <MenuItem value="instance">{t('adminLocations.mode.instance')}</MenuItem>
                <MenuItem value="kind">{t('adminLocations.mode.kind')}</MenuItem>
              </TextField>
            </Stack>

            {editing && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Switch
                  checked={draft.accountActive}
                  onChange={(e) => setDraft((prev) => ({ ...prev, accountActive: e.target.checked }))}
                />
                <Typography variant="body2">{t('adminLocations.modal.active')}</Typography>
              </Box>
            )}

            {/* Contato */}
            <Divider>
              <Typography variant="caption" color="text.secondary">{t('adminLocations.section.contact')}</Typography>
            </Divider>
            <Box>
              <Stack spacing={1}>
                {draft.telecom.map((tc, idx) => (
                  <Stack key={idx} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
                    <TextField
                      select
                      label={t('adminLocations.telecom.type')}
                      value={tc.system}
                      onChange={(e) => {
                        const updated = [...draft.telecom];
                        updated[idx] = { ...updated[idx], system: e.target.value as TelecomSystem };
                        setDraft((prev) => ({ ...prev, telecom: updated }));
                      }}
                      size="small"
                      sx={{ minWidth: 130 }}
                    >
                      <MenuItem value="phone">{t('adminLocations.telecom.system.phone')}</MenuItem>
                      <MenuItem value="fax">{t('adminLocations.telecom.system.fax')}</MenuItem>
                      <MenuItem value="email">{t('adminLocations.telecom.system.email')}</MenuItem>
                      <MenuItem value="url">{t('adminLocations.telecom.system.url')}</MenuItem>
                      <MenuItem value="sms">{t('adminLocations.telecom.system.sms')}</MenuItem>
                      <MenuItem value="other">{t('adminLocations.telecom.system.other')}</MenuItem>
                    </TextField>
                    <TextField
                      label={t('adminLocations.telecom.value')}
                      value={tc.value}
                      onChange={(e) => {
                        const updated = [...draft.telecom];
                        updated[idx] = { ...updated[idx], value: e.target.value };
                        setDraft((prev) => ({ ...prev, telecom: updated }));
                      }}
                      size="small"
                      fullWidth
                      placeholder={tc.system === 'phone' ? '+55 11 3000-0000' : tc.system === 'email' ? 'contato@clinica.com.br' : ''}
                    />
                    <TextField
                      select
                      label={t('adminLocations.telecom.use')}
                      value={tc.use}
                      onChange={(e) => {
                        const updated = [...draft.telecom];
                        updated[idx] = { ...updated[idx], use: e.target.value as TelecomUse };
                        setDraft((prev) => ({ ...prev, telecom: updated }));
                      }}
                      size="small"
                      sx={{ minWidth: 130 }}
                    >
                      <MenuItem value="work">{t('adminLocations.telecom.useOption.work')}</MenuItem>
                      <MenuItem value="home">{t('adminLocations.telecom.useOption.home')}</MenuItem>
                      <MenuItem value="mobile">{t('adminLocations.telecom.useOption.mobile')}</MenuItem>
                      <MenuItem value="temp">{t('adminLocations.telecom.useOption.temp')}</MenuItem>
                      <MenuItem value="old">{t('adminLocations.telecom.useOption.old')}</MenuItem>
                    </TextField>
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDraft((prev) => ({ ...prev, telecom: prev.telecom.filter((_, i) => i !== idx) }))}
                      aria-label={t('adminLocations.telecom.remove')}
                      sx={{ mt: 0.5 }}
                    >
                      <RemoveCircleOutlineIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                ))}
                <Button
                  size="small"
                  startIcon={<AddCircleOutlineIcon />}
                  onClick={() => setDraft((prev) => ({ ...prev, telecom: [...prev.telecom, { ...EMPTY_TELECOM }] }))}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {t('adminLocations.telecom.add')}
                </Button>
              </Stack>
            </Box>

            {/* Endereço */}
            <Divider>
              <Typography variant="caption" color="text.secondary">{t('adminLocations.section.address')}</Typography>
            </Divider>
            <Stack spacing={1.5}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('adminLocations.address.street')}
                  value={draft.address.line}
                  onChange={(e) => setDraft((prev) => ({ ...prev, address: { ...prev.address, line: e.target.value } }))}
                  size="small"
                  fullWidth
                  placeholder="Av. Paulista, 1000"
                />
                <TextField
                  label={t('adminLocations.address.complement')}
                  value={draft.address.complement}
                  onChange={(e) => setDraft((prev) => ({ ...prev, address: { ...prev.address, complement: e.target.value } }))}
                  size="small"
                  fullWidth
                  placeholder="Sala 101, Bela Vista"
                />
              </Stack>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('adminLocations.address.city')}
                  value={draft.address.city}
                  onChange={(e) => setDraft((prev) => ({ ...prev, address: { ...prev.address, city: e.target.value } }))}
                  size="small"
                  fullWidth
                  placeholder="São Paulo"
                />
                <TextField
                  label={t('adminLocations.address.state')}
                  value={draft.address.state}
                  onChange={(e) => setDraft((prev) => ({ ...prev, address: { ...prev.address, state: e.target.value } }))}
                  size="small"
                  inputProps={{ maxLength: 2 }}
                  placeholder="SP"
                  sx={{ minWidth: 100, maxWidth: 100 }}
                />
                <TextField
                  label={t('adminLocations.address.postalCode')}
                  value={draft.address.postalCode}
                  onChange={(e) => setDraft((prev) => ({ ...prev, address: { ...prev.address, postalCode: e.target.value } }))}
                  size="small"
                  inputProps={{ maxLength: 9 }}
                  placeholder="01310-100"
                  sx={{ minWidth: 150 }}
                />
              </Stack>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={loading}>{t('adminLocations.actions.cancel')}</Button>
          <Button variant="contained" onClick={handleSave} disabled={loading || !draft.displayName.trim()}>
            {editing ? t('adminLocations.actions.save') : t('adminLocations.actions.create')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
