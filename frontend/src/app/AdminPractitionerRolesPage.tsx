/**
 * T179d AdminPractitionerRolesPage — CRUD de PractitionerRole com todos os
 * campos FHIR opcionais mapeados: fhirCodeJson, fhirSpecialtyJson,
 * fhirTelecomJson, fhirAvailableTimeJson, primaryRole, periodStart, periodEnd.
 *
 * Refs: FR-013, FR-019, FR-020
 */

import { useCallback, useEffect, useState } from 'react';
import type { ChangeEvent } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import IconButton from '@mui/material/IconButton';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
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
import {
  listAdminPractitionerRoles,
  createAdminPractitionerRole,
  updateAdminPractitionerRole,
  deleteAdminPractitionerRole,
  type AdminPractitionerRole,
} from '../services/adminPractitionerRoleApi';
import { useTenant } from '../context/TenantContext';

// ── FHIR structured types ─────────────────────────────────────────────────────

type RoleTelecomSystem = 'phone' | 'fax' | 'email' | 'url' | 'sms' | 'other';
type RoleTelecomUse = 'work' | 'home' | 'mobile' | 'old' | 'temp';

interface RoleTelecomEntry { system: RoleTelecomSystem; value: string; use: RoleTelecomUse; }
const EMPTY_ROLE_TELECOM: RoleTelecomEntry = { system: 'phone', value: '', use: 'work' };

type DayCode = 'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun';
interface AvailableSlot { days: DayCode[]; startTime: string; endTime: string; }
const EMPTY_SLOT: AvailableSlot = { days: [], startTime: '08:00', endTime: '17:00' };

const DAY_LABELS: Record<DayCode, string> = { mon: 'Seg', tue: 'Ter', wed: 'Qua', thu: 'Qui', fri: 'Sex', sat: 'Sáb', sun: 'Dom' };
const ALL_DAYS: DayCode[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];

// Common FHIR role codes (PractitionerRole.code)
const FHIR_ROLE_CODE_OPTIONS = [
  { code: '309343006', display: 'Médico (Physician)' },
  { code: '224535009', display: 'Enfermeiro(a)' },
  { code: '46255001', display: 'Farmacêutico(a)' },
  { code: '159026005', display: 'Cirurgião(ã)-Dentista' },
  { code: '149968002', display: 'Fisioterapeuta' },
  { code: '106289002', display: 'Dentista' },
  { code: '26031000', display: 'Psicólogo(a)' },
  { code: '21450003', display: 'Psiquiatra' },
  { code: '17561000', display: 'Cardiologista' },
  { code: '3842006', display: 'Quiropraxista' },
  { code: 'OTHER', display: 'Outra função (especificar)' },
];

// Common specialty codes (SNOMED-based CFM specialties)
const SPECIALTY_OPTIONS = [
  { code: '394814009', display: 'Clínica Geral' },
  { code: '394579002', display: 'Cardiologia' },
  { code: '394812008', display: 'Dermatologia' },
  { code: '394803006', display: 'Endocrinologia' },
  { code: '394805004', display: 'Gastroenterologia' },
  { code: '394807007', display: 'Ginecologia e Obstetrícia' },
  { code: '408480009', display: 'Medicina Intensiva' },
  { code: '394589003', display: 'Nefrologia' },
  { code: '394591006', display: 'Neurologia' },
  { code: '394596001', display: 'Oftalmologia' },
  { code: '394598007', display: 'Oncologia' },
  { code: '394801008', display: 'Ortopedia' },
  { code: '394604002', display: 'Otorrinolaringologia' },
  { code: '394611003', display: 'Pediatria' },
  { code: '418960008', display: 'Pneumologia' },
  { code: '394737007', display: 'Psiquiatria' },
  { code: '394810000', display: 'Radiologia' },
  { code: '394609007', display: 'Reumatologia' },
  { code: '394620008', display: 'Urologia' },
  { code: 'OTHER', display: 'Outra especialidade (especificar)' },
];

// ── JSON builders ─────────────────────────────────────────────────────────────

function buildRoleTelecomJson(entries: RoleTelecomEntry[]): string | undefined {
  const v = entries.filter((e) => e.value.trim());
  return v.length > 0 ? JSON.stringify(v.map((e) => ({ system: e.system, value: e.value.trim(), use: e.use }))) : undefined;
}

function buildRoleCodeJson(code: string, customDisplay: string): string | undefined {
  if (!code) return undefined;
  const option = FHIR_ROLE_CODE_OPTIONS.find((o) => o.code === code);
  const display = code === 'OTHER' ? customDisplay.trim() : (option?.display ?? code);
  const snomedCode = code === 'OTHER' ? customDisplay.trim() : code;
  return JSON.stringify([{ coding: [{ system: 'http://snomed.info/sct', code: snomedCode, display }] }]);
}

function buildSpecialtyJson(code: string, customDisplay: string): string | undefined {
  if (!code) return undefined;
  const option = SPECIALTY_OPTIONS.find((o) => o.code === code);
  const display = code === 'OTHER' ? customDisplay.trim() : (option?.display ?? code);
  const snomedCode = code === 'OTHER' ? customDisplay.trim() : code;
  return JSON.stringify([{ coding: [{ system: 'http://snomed.info/sct', code: snomedCode, display }] }]);
}

function buildAvailableTimeJson(slots: AvailableSlot[]): string | undefined {
  const valid = slots.filter((s) => s.days.length > 0 && s.startTime && s.endTime);
  if (valid.length === 0) return undefined;
  return JSON.stringify(valid.map((s) => ({
    daysOfWeek: s.days,
    availableStartTime: s.startTime + ':00',
    availableEndTime: s.endTime + ':00',
  })));
}

// ── Parsers ───────────────────────────────────────────────────────────────────

function parseRoleTelecom(json: string | undefined | null): RoleTelecomEntry[] {
  try { return (JSON.parse(json ?? '[]') as RoleTelecomEntry[]); }
  catch { return []; }
}

function parseRoleCode(json: string | undefined | null): { code: string; customDisplay: string } {
  try {
    const arr = JSON.parse(json ?? '[]') as Array<{ coding?: Array<{ code?: string; display?: string }> }>;
    const c = arr[0]?.coding?.[0];
    if (!c?.code) return { code: '', customDisplay: '' };
    const known = FHIR_ROLE_CODE_OPTIONS.find((o) => o.code === c.code);
    return known ? { code: c.code, customDisplay: '' } : { code: 'OTHER', customDisplay: c.display ?? c.code ?? '' };
  } catch { return { code: '', customDisplay: '' }; }
}

function parseSpecialty(json: string | undefined | null): { code: string; customDisplay: string } {
  try {
    const arr = JSON.parse(json ?? '[]') as Array<{ coding?: Array<{ code?: string; display?: string }> }>;
    const c = arr[0]?.coding?.[0];
    if (!c?.code) return { code: '', customDisplay: '' };
    const known = SPECIALTY_OPTIONS.find((o) => o.code === c.code);
    return known ? { code: c.code, customDisplay: '' } : { code: 'OTHER', customDisplay: c.display ?? c.code ?? '' };
  } catch { return { code: '', customDisplay: '' }; }
}

function parseAvailableTime(json: string | undefined | null): AvailableSlot[] {
  try {
    const arr = JSON.parse(json ?? '[]') as Array<{ daysOfWeek?: string[]; availableStartTime?: string; availableEndTime?: string }>;
    return arr.map((a) => ({
      days: (a.daysOfWeek ?? []) as DayCode[],
      startTime: (a.availableStartTime ?? '08:00:00').substring(0, 5),
      endTime: (a.availableEndTime ?? '17:00:00').substring(0, 5),
    }));
  } catch { return []; }
}

// ── Draft (form state) ────────────────────────────────────────────────────────

interface RoleDraft {
  id: string;
  organizationId: string;
  locationId: string;
  practitionerId: string;
  roleCode: string;
  active: boolean;
  primaryRole: boolean;
  periodStart: string;
  periodEnd: string;
  // structured FHIR
  fhirRoleCode: string;
  fhirRoleCodeCustom: string;
  fhirSpecialty: string;
  fhirSpecialtyCustom: string;
  fhirTelecom: RoleTelecomEntry[];
  fhirAvailableSlots: AvailableSlot[];
}

const EMPTY_DRAFT: RoleDraft = {
  id: '',
  organizationId: '',
  locationId: '',
  practitionerId: '',
  roleCode: '',
  active: true,
  primaryRole: false,
  periodStart: '',
  periodEnd: '',
  fhirRoleCode: '',
  fhirRoleCodeCustom: '',
  fhirSpecialty: '',
  fhirSpecialtyCustom: '',
  fhirTelecom: [],
  fhirAvailableSlots: [],
};

function toIsoOrUndefined(dateStr: string): string | undefined {
  if (!dateStr.trim()) return undefined;
  try {
    return new Date(dateStr.trim()).toISOString();
  } catch {
    return undefined;
  }
}

export function AdminPractitionerRolesPage() {
  const tenant = useTenant();
  const tenantId = tenant.tenantId ?? '';
  const [rows, setRows] = useState<AdminPractitionerRole[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [draft, setDraft] = useState<RoleDraft>(EMPTY_DRAFT);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    setErrorMsg(null);
    if (!tenantId) {
      setRows([]);
      setLoading(false);
      return;
    }
    listAdminPractitionerRoles(tenantId)
      .then(setRows)
      .catch((err) => setErrorMsg(String(err?.message ?? 'Erro ao carregar funções')))
      .finally(() => setLoading(false));
  }, [tenantId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function openCreate() {
    setDraft(EMPTY_DRAFT);
    setIsEdit(false);
    setDialogOpen(true);
  }

  function openEdit(role: AdminPractitionerRole) {
    const rc = parseRoleCode(role.fhirCodeJson);
    const sp = parseSpecialty(role.fhirSpecialtyJson);
    setDraft({
      id: role.id,
      organizationId: role.organizationId,
      locationId: role.locationId,
      practitionerId: role.practitionerId,
      roleCode: role.roleCode,
      active: role.active,
      primaryRole: role.primaryRole,
      periodStart: role.periodStart ?? '',
      periodEnd: role.periodEnd ?? '',
      fhirRoleCode: rc.code,
      fhirRoleCodeCustom: rc.customDisplay,
      fhirSpecialty: sp.code,
      fhirSpecialtyCustom: sp.customDisplay,
      fhirTelecom: parseRoleTelecom(role.fhirTelecomJson),
      fhirAvailableSlots: parseAvailableTime(role.fhirAvailableTimeJson),
    });
    setIsEdit(true);
    setDialogOpen(true);
  }

  function closeDialog() {
    setDialogOpen(false);
    setDraft(EMPTY_DRAFT);
  }

  async function handleSave() {
    setLoading(true);
    setErrorMsg(null);
    try {
      const fhirCodeJson = buildRoleCodeJson(draft.fhirRoleCode, draft.fhirRoleCodeCustom);
      const fhirSpecialtyJson = buildSpecialtyJson(draft.fhirSpecialty, draft.fhirSpecialtyCustom);
      const fhirTelecomJson = buildRoleTelecomJson(draft.fhirTelecom);
      const fhirAvailableTimeJson = buildAvailableTimeJson(draft.fhirAvailableSlots);
      if (isEdit) {
        await updateAdminPractitionerRole(tenantId, draft.id, {
          roleCode: draft.roleCode.trim() || undefined,
          active: draft.active,
          primaryRole: draft.primaryRole,
          periodStart: toIsoOrUndefined(draft.periodStart),
          periodEnd: toIsoOrUndefined(draft.periodEnd),
          fhirCodeJson,
          fhirSpecialtyJson,
          fhirTelecomJson,
          fhirAvailableTimeJson,
        });
      } else {
        await createAdminPractitionerRole(tenantId, {
          organizationId: draft.organizationId.trim(),
          locationId: draft.locationId.trim(),
          practitionerId: draft.practitionerId.trim(),
          roleCode: draft.roleCode.trim(),
          primaryRole: draft.primaryRole,
          periodStart: toIsoOrUndefined(draft.periodStart),
          periodEnd: toIsoOrUndefined(draft.periodEnd),
          fhirCodeJson,
          fhirSpecialtyJson,
          fhirTelecomJson,
          fhirAvailableTimeJson,
        });
      }
      closeDialog();
      refresh();
    } catch (err: unknown) {
      setErrorMsg(String((err as Error)?.message ?? 'Erro ao salvar'));
    } finally {
      setLoading(false);
    }
  }

  function field(key: keyof RoleDraft) {
    return (e: ChangeEvent<HTMLInputElement>) =>
      setDraft((prev) => ({ ...prev, [key]: e.target.value }));
  }

  async function handleDeactivate(roleId: string) {
    if (!window.confirm('Deseja desativar esta função de profissional?')) {
      return;
    }
    setLoading(true);
    setErrorMsg(null);
    try {
      await deleteAdminPractitionerRole(tenantId, roleId);
      refresh();
    } catch (err: unknown) {
      setErrorMsg(String((err as Error)?.message ?? 'Erro ao desativar'));
    } finally {
      setLoading(false);
    }
  }

  const canSave = isEdit
    ? draft.roleCode.trim().length > 0
    : draft.organizationId.trim().length > 0 &&
      draft.locationId.trim().length > 0 &&
      draft.practitionerId.trim().length > 0 &&
      draft.roleCode.trim().length > 0;

  return (
    <Box data-testid="admin-practitioner-roles-page">
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Funções de Profissional (PractitionerRole)</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Recarregar">
            <span>
              <IconButton onClick={refresh} disabled={loading} aria-label="Recarregar">
                <RefreshIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Nova Função
          </Button>
        </Box>
      </Box>

      {errorMsg && (
        <Alert severity="error" onClose={() => setErrorMsg(null)} sx={{ mb: 2 }}>
          {errorMsg}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Profissional ID</TableCell>
              <TableCell>Código de Função</TableCell>
              <TableCell>Localização</TableCell>
              <TableCell>Principal</TableCell>
              <TableCell>Ativo</TableCell>
              <TableCell>Período</TableCell>
              <TableCell align="right">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                  {loading ? 'Carregando...' : 'Nenhuma função cadastrada'}
                </TableCell>
              </TableRow>
            ) : (
              rows.map((role) => (
                <TableRow key={role.id}>
                  <TableCell>
                    <Typography variant="caption" fontFamily="monospace">
                      {role.practitionerId.substring(0, 8)}…
                    </Typography>
                  </TableCell>
                  <TableCell>{role.roleCode}</TableCell>
                  <TableCell>
                    <Typography variant="caption" fontFamily="monospace">
                      {role.locationId.substring(0, 8)}…
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {role.primaryRole ? (
                      <Chip label="Principal" color="primary" size="small" />
                    ) : (
                      <Chip label="Secundária" size="small" />
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={role.active ? 'Ativo' : 'Inativo'}
                      color={role.active ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">
                      {role.periodStart ? new Date(role.periodStart).toLocaleDateString('pt-BR') : '—'}
                      {' → '}
                      {role.periodEnd ? new Date(role.periodEnd).toLocaleDateString('pt-BR') : '∞'}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Editar">
                      <IconButton size="small" onClick={() => openEdit(role)} aria-label="Editar">
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Desativar">
                      <IconButton size="small" color="error" onClick={() => handleDeactivate(role.id)} aria-label="Desativar">
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

      <Dialog open={dialogOpen} onClose={closeDialog} fullWidth maxWidth="md">
        <DialogTitle>{isEdit ? 'Editar Função de Profissional' : 'Nova Função de Profissional'}</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1 }}>
            <Stack spacing={2}>
              {!isEdit && (
                <>
                  <TextField
                    label="Practitioner ID (UUID)"
                    value={draft.practitionerId}
                    onChange={field('practitionerId')}
                    size="small"
                    required
                    fullWidth
                    placeholder="UUID do profissional de saúde"
                  />
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                      label="Organization ID (UUID)"
                      value={draft.organizationId}
                      onChange={field('organizationId')}
                      size="small"
                      required
                      fullWidth
                      placeholder="UUID da organização"
                    />
                    <TextField
                      label="Location ID (UUID)"
                      value={draft.locationId}
                      onChange={field('locationId')}
                      size="small"
                      required
                      fullWidth
                      placeholder="UUID da unidade/localização"
                    />
                  </Stack>
                </>
              )}

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
                <TextField
                  label="Código de Função (roleCode)"
                  value={draft.roleCode}
                  onChange={field('roleCode')}
                  size="small"
                  required
                  fullWidth
                  placeholder="MD, RN, NP..."
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={draft.primaryRole}
                      onChange={(e) => setDraft((prev) => ({ ...prev, primaryRole: e.target.checked }))}
                    />
                  }
                  label="Função Principal"
                />
                {isEdit && (
                  <FormControlLabel
                    control={
                      <Switch
                        checked={draft.active}
                        onChange={(e) => setDraft((prev) => ({ ...prev, active: e.target.checked }))}
                      />
                    }
                    label="Ativo"
                  />
                )}
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label="Início do Período"
                  type="datetime-local"
                  value={draft.periodStart}
                  onChange={field('periodStart')}
                  size="small"
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  helperText="Opcional — PractitionerRole.period.start"
                />
                <TextField
                  label="Fim do Período"
                  type="datetime-local"
                  value={draft.periodEnd}
                  onChange={field('periodEnd')}
                  size="small"
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  helperText="Opcional — PractitionerRole.period.end"
                />
              </Stack>

              {/* ── Função FHIR ────────────────────────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">Tipo de Função — opcional</Typography>
              </Divider>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  select label="Tipo de Função (FHIR Code)"
                  value={draft.fhirRoleCode}
                  onChange={(e) => setDraft((p) => ({ ...p, fhirRoleCode: e.target.value }))}
                  size="small" fullWidth
                >
                  <MenuItem value="">— Não informado —</MenuItem>
                  {FHIR_ROLE_CODE_OPTIONS.map((o) => (
                    <MenuItem key={o.code} value={o.code}>{o.display}</MenuItem>
                  ))}
                </TextField>
                <TextField
                  select label="Especialidade (FHIR Specialty)"
                  value={draft.fhirSpecialty}
                  onChange={(e) => setDraft((p) => ({ ...p, fhirSpecialty: e.target.value }))}
                  size="small" fullWidth
                >
                  <MenuItem value="">— Não informado —</MenuItem>
                  {SPECIALTY_OPTIONS.map((o) => (
                    <MenuItem key={o.code} value={o.code}>{o.display}</MenuItem>
                  ))}
                </TextField>
              </Stack>

              {(draft.fhirRoleCode === 'OTHER' || draft.fhirSpecialty === 'OTHER') && (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  {draft.fhirRoleCode === 'OTHER' && (
                    <TextField
                      label="Função (descrição livre)"
                      value={draft.fhirRoleCodeCustom}
                      onChange={(e) => setDraft((p) => ({ ...p, fhirRoleCodeCustom: e.target.value }))}
                      size="small" fullWidth placeholder="Ex: Técnico de Radiologia"
                    />
                  )}
                  {draft.fhirSpecialty === 'OTHER' && (
                    <TextField
                      label="Especialidade (descrição livre)"
                      value={draft.fhirSpecialtyCustom}
                      onChange={(e) => setDraft((p) => ({ ...p, fhirSpecialtyCustom: e.target.value }))}
                      size="small" fullWidth placeholder="Ex: Medicina do Trabalho"
                    />
                  )}
                </Stack>
              )}

              {/* ── Contatos ────────────────────────────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">Contatos do Papel — opcional</Typography>
              </Divider>

              <Box>
                <Stack spacing={1}>
                  {draft.fhirTelecom.map((tc, idx) => (
                    <Stack key={idx} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
                      <TextField
                        select label="Tipo" value={tc.system} size="small"
                        onChange={(e) => {
                          const updated = [...draft.fhirTelecom];
                          updated[idx] = { ...updated[idx], system: e.target.value as RoleTelecomSystem };
                          setDraft((p) => ({ ...p, fhirTelecom: updated }));
                        }}
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
                        label="Número / endereço" value={tc.value} size="small" fullWidth
                        onChange={(e) => {
                          const updated = [...draft.fhirTelecom];
                          updated[idx] = { ...updated[idx], value: e.target.value };
                          setDraft((p) => ({ ...p, fhirTelecom: updated }));
                        }}
                      />
                      <TextField
                        select label="Uso" value={tc.use} size="small"
                        onChange={(e) => {
                          const updated = [...draft.fhirTelecom];
                          updated[idx] = { ...updated[idx], use: e.target.value as RoleTelecomUse };
                          setDraft((p) => ({ ...p, fhirTelecom: updated }));
                        }}
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="work">Trabalho</MenuItem>
                        <MenuItem value="home">Residencial</MenuItem>
                        <MenuItem value="mobile">Celular</MenuItem>
                        <MenuItem value="temp">Temporário</MenuItem>
                        <MenuItem value="old">Antigo</MenuItem>
                      </TextField>
                      <IconButton size="small" color="error" sx={{ mt: 0.5 }}
                        onClick={() => setDraft((p) => ({ ...p, fhirTelecom: p.fhirTelecom.filter((_, i) => i !== idx) }))}
                        aria-label="Remover contato"
                      >
                        <RemoveCircleOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                  <Button size="small" startIcon={<AddCircleOutlineIcon />} sx={{ alignSelf: 'flex-start' }}
                    onClick={() => setDraft((p) => ({ ...p, fhirTelecom: [...p.fhirTelecom, { ...EMPTY_ROLE_TELECOM }] }))}
                  >
                    Adicionar contato
                  </Button>
                </Stack>
              </Box>

              {/* ── Horários de atendimento ──────────────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">Horários de Atendimento — opcional</Typography>
              </Divider>

              <Box>
                <Stack spacing={2}>
                  {draft.fhirAvailableSlots.map((slot, idx) => (
                    <Box key={idx} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                        <Typography variant="caption" color="text.secondary" sx={{ mb: 1 }}>Faixa de horário {idx + 1}</Typography>
                        <IconButton size="small" color="error"
                          onClick={() => setDraft((p) => ({ ...p, fhirAvailableSlots: p.fhirAvailableSlots.filter((_, i) => i !== idx) }))}
                          aria-label="Remover faixa"
                        >
                          <RemoveCircleOutlineIcon fontSize="small" />
                        </IconButton>
                      </Stack>
                      <FormGroup row sx={{ mb: 1 }}>
                        {ALL_DAYS.map((day) => (
                          <FormControlLabel
                            key={day}
                            label={DAY_LABELS[day]}
                            control={
                              <Checkbox
                                size="small"
                                checked={slot.days.includes(day)}
                                onChange={(e) => {
                                  const updated = [...draft.fhirAvailableSlots];
                                  const days = e.target.checked
                                    ? [...slot.days, day]
                                    : slot.days.filter((d) => d !== day);
                                  updated[idx] = { ...updated[idx], days };
                                  setDraft((p) => ({ ...p, fhirAvailableSlots: updated }));
                                }}
                              />
                            }
                          />
                        ))}
                      </FormGroup>
                      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField
                          label="Início" type="time" value={slot.startTime} size="small"
                          onChange={(e) => {
                            const updated = [...draft.fhirAvailableSlots];
                            updated[idx] = { ...updated[idx], startTime: e.target.value };
                            setDraft((p) => ({ ...p, fhirAvailableSlots: updated }));
                          }}
                          InputLabelProps={{ shrink: true }}
                          sx={{ maxWidth: 150 }}
                        />
                        <TextField
                          label="Fim" type="time" value={slot.endTime} size="small"
                          onChange={(e) => {
                            const updated = [...draft.fhirAvailableSlots];
                            updated[idx] = { ...updated[idx], endTime: e.target.value };
                            setDraft((p) => ({ ...p, fhirAvailableSlots: updated }));
                          }}
                          InputLabelProps={{ shrink: true }}
                          sx={{ maxWidth: 150 }}
                        />
                      </Stack>
                    </Box>
                  ))}
                  <Button size="small" startIcon={<AddCircleOutlineIcon />} sx={{ alignSelf: 'flex-start' }}
                    onClick={() => setDraft((p) => ({ ...p, fhirAvailableSlots: [...p.fhirAvailableSlots, { ...EMPTY_SLOT }] }))}
                  >
                    Adicionar faixa de horário
                  </Button>
                </Stack>
              </Box>
            </Stack>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={loading}>
            Cancelar
          </Button>
          <Button onClick={handleSave} variant="contained" disabled={loading || !canSave}>
            {loading ? 'Salvando...' : isEdit ? 'Salvar' : 'Criar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
