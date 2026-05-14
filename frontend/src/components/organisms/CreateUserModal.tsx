/**
 * T097 CreateUserModal — admin form to create or edit a profile-20 user.
 */

import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  InputAdornment,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { AddCircleOutline as AddCircleOutlineIcon, RemoveCircleOutline as RemoveCircleOutlineIcon } from '@mui/icons-material';
import { useTranslation } from 'react-i18next';
import { createProfile20User, type IamAuthError } from '../../services/iamAuthApi';
import { useAuth } from '../../context/AuthContext';

// ── Editable user shape (passed when editing existing user) ───────────────────

export interface EditableUser {
  id: string;
  email: string;
  username: string;
  accountActive: boolean;
}

// ── FHIR structured types ─────────────────────────────────────────────────────

type PractTelecomSystem = 'phone' | 'fax' | 'email' | 'pager' | 'url' | 'sms' | 'other';
type PractTelecomUse = 'work' | 'home' | 'mobile' | 'old' | 'temp';

interface PractTelecomEntry { system: PractTelecomSystem; value: string; use: PractTelecomUse; }
interface PractAddressEntry { use: 'work' | 'home' | 'billing' | 'old' | 'temp'; line: string; complement: string; city: string; state: string; postalCode: string; country: string; }
interface QualificationEntry { system: string; code: string; display: string; }

const EMPTY_PRACT_TELECOM: PractTelecomEntry = { system: 'phone', value: '', use: 'work' };
const EMPTY_PRACT_ADDRESS: PractAddressEntry = { use: 'home', line: '', complement: '', city: '', state: '', postalCode: '', country: 'BR' };
const EMPTY_QUALIFICATION: QualificationEntry = { system: 'CRM', code: '', display: '' };

// JSON builders
function buildPractTelecomJson(entries: PractTelecomEntry[]): string | undefined {
  const v = entries.filter((e) => e.value.trim());
  return v.length > 0 ? JSON.stringify(v.map((e) => ({ system: e.system, value: e.value.trim(), use: e.use }))) : undefined;
}
function buildPractAddressJson(addr: PractAddressEntry): string | undefined {
  if (!addr.line.trim() && !addr.city.trim()) return undefined;
  const lines = [addr.line.trim(), addr.complement.trim()].filter(Boolean);
  return JSON.stringify([{ use: addr.use, line: lines, city: addr.city.trim(), state: addr.state.trim(), postalCode: addr.postalCode.trim(), country: addr.country.trim() || 'BR' }]);
}
function buildQualificationJson(qs: QualificationEntry[]): string | undefined {
  const v = qs.filter((q) => q.code.trim());
  if (v.length === 0) return undefined;
  return JSON.stringify(v.map((q) => ({
    code: { coding: [{ system: qualSystemUri(q.system), code: q.code.trim(), display: q.display.trim() || undefined }] },
  })));
}
function qualSystemUri(system: string): string {
  const map: Record<string, string> = {
    CRM: 'https://www.crm.org.br',
    CRO: 'https://cfo.org.br',
    COREN: 'https://www.cofen.gov.br',
    CRF: 'https://www.cff.org.br',
    CFP: 'https://cfp.org.br',
    CRP: 'https://cfp.org.br',
    OUTRO: 'urn:oid:2.16.840.1.113883.2.4.15.111',
  };
  return map[system] ?? system;
}

// ── User detail DTO (returned by GET /api/admin/users/:id) ────────────────────

interface UserDetailDto {
  id: string;
  email: string;
  username: string;
  profileType: number;
  accountActive: boolean;
  createdAt: string | null;
  fhirGender: string | null;
  fhirBirthDate: string | null;
  fhirTelecomJson: string | null;
  fhirAddressJson: string | null;
  fhirQualificationJson: string | null;
  fhirCommunicationJson: string | null;
  cpf: string | null;
  locationId: string | null;
  roleCode: string | null;
}

// ── FHIR JSON parsers ─────────────────────────────────────────────────────────

function parseFhirTelecom(json: string | null | undefined): PractTelecomEntry[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json) as { system: PractTelecomSystem; value: string; use: PractTelecomUse }[];
    return arr.map((e) => ({ system: e.system ?? 'phone', value: e.value ?? '', use: e.use ?? 'work' }));
  } catch { return []; }
}

function parseFhirAddress(json: string | null | undefined): PractAddressEntry {
  if (!json) return { ...EMPTY_PRACT_ADDRESS };
  try {
    const arr = JSON.parse(json) as { use?: string; line?: string[]; city?: string; state?: string; postalCode?: string; country?: string }[];
    const first = arr[0] ?? {};
    return {
      use: (first.use as PractAddressEntry['use']) ?? 'home',
      line: first.line?.[0] ?? '',
      complement: first.line?.[1] ?? '',
      city: first.city ?? '',
      state: first.state ?? '',
      postalCode: first.postalCode ?? '',
      country: first.country ?? 'BR',
    };
  } catch { return { ...EMPTY_PRACT_ADDRESS }; }
}

function parseFhirQualifications(json: string | null | undefined): QualificationEntry[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json) as { code?: { coding?: { system?: string; code?: string; display?: string }[] } }[];
    const systemMap: Record<string, string> = {
      'https://www.crm.org.br': 'CRM', 'https://cfo.org.br': 'CRO',
      'https://www.cofen.gov.br': 'COREN', 'https://www.cff.org.br': 'CRF',
      'https://cfp.org.br': 'CRP',
    };
    return arr.map((q) => {
      const coding = q.code?.coding?.[0] ?? {};
      return { system: systemMap[coding.system ?? ''] ?? 'OUTRO', code: coding.code ?? '', display: coding.display ?? '' };
    });
  } catch { return []; }
}

function parseFhirCommunicationLang(json: string | null | undefined): string {
  if (!json) return '';
  try {
    const arr = JSON.parse(json) as { coding?: { code?: string }[] }[];
    return arr[0]?.coding?.[0]?.code ?? '';
  } catch { return ''; }
}

// ── Form state ────────────────────────────────────────────────────────────────

interface FormState {
  displayName: string;
  email: string;
  cpf: string;
  password: string;
  active: boolean;
  // FHIR Practitioner optional
  fhirGender: string;
  fhirBirthDate: string;
  fhirTelecom: PractTelecomEntry[];
  fhirAddress: PractAddressEntry;
  fhirQualifications: QualificationEntry[];
  fhirCommunicationLang: string;  // BCP-47 language code
}

const EMPTY_FORM: FormState = {
  displayName: '',
  email: '',
  cpf: '',
  password: '',
  active: true,
  fhirGender: '',
  fhirBirthDate: '',
  fhirTelecom: [],
  fhirAddress: { ...EMPTY_PRACT_ADDRESS },
  fhirQualifications: [],
  fhirCommunicationLang: '',
};

export interface UserLocationOption {
  id: string;
  label: string;
}

export interface UserRoleOption {
  code: string;
  label: string;
}

interface CreateUserModalProps {
  open: boolean;
  onClose: (success?: boolean) => void;
  locationId?: string;
  roleCode?: string;
  locationOptions?: UserLocationOption[];
  roleOptions?: UserRoleOption[];
  editUser?: EditableUser;
}

export function CreateUserModal({
  open,
  onClose,
  locationId,
  roleCode,
  locationOptions = [],
  roleOptions = [],
  editUser,
}: CreateUserModalProps): React.ReactElement {
  const { t } = useTranslation();
  const { session } = useAuth();
  const isEditMode = editUser != null;

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [selectedLocationId, setSelectedLocationId] = useState(locationId ?? '');
  const [selectedRoleCode, setSelectedRoleCode] = useState(roleCode ?? '');

  useEffect(() => {
    if (!open) return;

    if (editUser) {
      setForm({
        ...EMPTY_FORM,
        displayName: editUser.username,
        email: editUser.email,
        active: editUser.accountActive,
      });

      const sessionId = session?.sessionId ?? '';
      const tenantId = session?.tenant?.id ?? '';
      fetch(`/api/admin/users/${editUser.id}`, {
        headers: {
          'X-Tenant-ID': tenantId,
          Authorization: `Bearer ${sessionId}`,
        },
      })
        .then((res) => res.ok ? (res.json() as Promise<UserDetailDto>) : null)
        .then((detail) => {
          if (!detail) return;
          setForm((prev) => ({
            ...prev,
            cpf: detail.cpf ?? '',
            fhirGender: detail.fhirGender ?? '',
            fhirBirthDate: detail.fhirBirthDate ?? '',
            fhirTelecom: parseFhirTelecom(detail.fhirTelecomJson),
            fhirAddress: parseFhirAddress(detail.fhirAddressJson),
            fhirQualifications: parseFhirQualifications(detail.fhirQualificationJson),
            fhirCommunicationLang: parseFhirCommunicationLang(detail.fhirCommunicationJson),
          }));
          if (detail.locationId) setSelectedLocationId(detail.locationId);
          if (detail.roleCode) setSelectedRoleCode(detail.roleCode);
        })
        .catch(() => { /* silently ignore; form stays with basic fields */ });
    } else {
      setForm(EMPTY_FORM);
    }

    setSuccessMsg(null);
    setErrorMsg(null);
    setSelectedLocationId(locationId ?? '');
    setSelectedRoleCode(roleCode ?? '');
  }, [editUser, open, locationId, roleCode, session]);

  function handleChange(field: keyof FormState) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
    };
  }

  function handleClose() {
    setForm(EMPTY_FORM);
    setSuccessMsg(null);
    setErrorMsg(null);
    onClose();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    const sessionId = session?.sessionId ?? '';
    const tenantId = session?.tenant?.id ?? '';

    try {
      if (isEditMode && editUser) {
        const body: Record<string, unknown> = {
          email: form.email,
          username: form.displayName,
          active: form.active,
          cpf: form.cpf && /^\d{11}$/.test(form.cpf) ? form.cpf : null,
          locationId: selectedLocationId || null,
          roleCode: selectedRoleCode || null,
          fhirGender: form.fhirGender || null,
          fhirBirthDate: form.fhirBirthDate || null,
          fhirTelecomJson: buildPractTelecomJson(form.fhirTelecom) ?? null,
          fhirAddressJson: buildPractAddressJson(form.fhirAddress) ?? null,
          fhirQualificationJson: buildQualificationJson(form.fhirQualifications) ?? null,
          fhirCommunicationJson: form.fhirCommunicationLang
            ? JSON.stringify([{ coding: [{ system: 'urn:ietf:bcp:47', code: form.fhirCommunicationLang }] }])
            : null,
        };

        const res = await fetch(`/api/admin/users/${editUser.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': tenantId,
            Authorization: `Bearer ${sessionId}`,
          },
          body: JSON.stringify(body),
        });

        if (!res.ok) {
          const errBody = await res.json().catch(() => ({}));
          throw new Error(errBody?.issue?.[0]?.diagnostics ?? t('createUserModal.error.update'));
        }

        setSuccessMsg(t('createUserModal.editSuccess'));
        setTimeout(() => {
          setSuccessMsg(null);
          onClose(true);
        }, 1200);
      } else {
        await createProfile20User(tenantId, {
          practitioner: {
            displayName: form.displayName,
            email: form.email,
            cpf: form.cpf,
            password: form.password,
            fhirTelecomJson: buildPractTelecomJson(form.fhirTelecom),
            fhirAddressJson: buildPractAddressJson(form.fhirAddress),
            fhirGender: form.fhirGender || undefined,
            fhirBirthDate: form.fhirBirthDate || undefined,
            fhirQualificationJson: buildQualificationJson(form.fhirQualifications),
            fhirCommunicationJson: form.fhirCommunicationLang
              ? JSON.stringify([{ coding: [{ system: 'urn:ietf:bcp:47', code: form.fhirCommunicationLang }] }])
              : undefined,
          },
          locationId: selectedLocationId,
          roleCode: selectedRoleCode,
        });

        setSuccessMsg(t('createUserModal.success', { name: form.displayName }));
        setForm(EMPTY_FORM);
        setTimeout(() => {
          setSuccessMsg(null);
          onClose(true);
        }, 1500);
      }
    } catch (err: unknown) {
      const authErr = err as IamAuthError;
      const diagnostics =
        (err instanceof Error ? err.message : null) ??
        authErr?.body?.issue?.[0]?.diagnostics ??
        (isEditMode ? t('createUserModal.error.update') : t('createUserModal.error.default'));
      setErrorMsg(String(diagnostics));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>{isEditMode ? t('createUserModal.editTitle') : t('createUserModal.title')}</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {successMsg && (
            <Alert severity="success" data-testid="toast-success">
              {successMsg}
            </Alert>
          )}
          {errorMsg && (
            <Alert severity="error" data-testid="toast-error">
              {errorMsg}
            </Alert>
          )}

          {/* ── Dados obrigatórios ──────────────────────────────────────── */}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label={t('createUserModal.field.displayName')}
              name="displayName"
              value={form.displayName}
              onChange={handleChange('displayName')}
              required
              autoFocus
              fullWidth
            />
            <TextField
              label={t('createUserModal.field.email')}
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange('email')}
              required
              fullWidth
            />
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label={t('createUserModal.field.cpf')}
              name="cpf"
              value={form.cpf}
              onChange={handleChange('cpf')}
              required={!isEditMode}
              inputProps={{ pattern: '\\d{11}', maxLength: 11 }}
              helperText={t('createUserModal.field.cpf.helper')}
              sx={{ minWidth: 180 }}
            />
            {!isEditMode && (
              <TextField
                label={t('createUserModal.field.password')}
                name="password"
                type="password"
                value={form.password}
                onChange={handleChange('password')}
                required
                fullWidth
              />
            )}
          </Stack>

          {isEditMode && (
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
              <FormControlLabel
                control={
                  <Switch
                    checked={form.active}
                    onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
                    name="active"
                  />
                }
                label={t('createUserModal.field.active')}
              />
            </Stack>
          )}

          {/* ── Dados de vínculo ───────────────────────────────────── */}
          <Divider sx={{ my: 1 }}>
            <Typography variant="caption" color="text.secondary">{t('createUserModal.section.location')}</Typography>
          </Divider>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              select
              label={t('createUserModal.field.locationId')}
              value={selectedLocationId}
              onChange={(e) => setSelectedLocationId(e.target.value)}
              required={!isEditMode}
              fullWidth
              helperText={locationOptions.length === 0 ? t('createUserModal.helper.noLocations') : ''}
            >
              {locationOptions.map((option) => (
                <MenuItem key={option.id} value={option.id}>{option.label}</MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label={t('createUserModal.field.roleCode')}
              value={selectedRoleCode}
              onChange={(e) => setSelectedRoleCode(e.target.value)}
              required={!isEditMode}
              fullWidth
            >
              <MenuItem value="">{t('createUserModal.field.roleCode.placeholder')}</MenuItem>
              {roleOptions.map((o) => (
                <MenuItem key={o.code} value={o.code}>{o.label || o.code}</MenuItem>
              ))}
            </TextField>
          </Stack>

          {/* ── Dados pessoais do profissional ─────────────────────── */}
          <Divider sx={{ my: 1 }}>
            <Typography variant="caption" color="text.secondary">{t('createUserModal.section.personal')}</Typography>
          </Divider>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              select
              label={t('createUserModal.field.gender')}
              value={form.fhirGender}
              onChange={(e) => setForm((prev) => ({ ...prev, fhirGender: e.target.value }))}
              fullWidth
            >
              <MenuItem value="">{t('createUserModal.field.gender.unknown')}</MenuItem>
              <MenuItem value="male">{t('createUserModal.field.gender.male')}</MenuItem>
              <MenuItem value="female">{t('createUserModal.field.gender.female')}</MenuItem>
              <MenuItem value="other">{t('createUserModal.field.gender.other')}</MenuItem>
              <MenuItem value="unknown">{t('createUserModal.field.gender.preferNotToSay')}</MenuItem>
            </TextField>
            <TextField
              label={t('createUserModal.field.birthDate')}
              type="date"
              value={form.fhirBirthDate}
              onChange={(e) => setForm((prev) => ({ ...prev, fhirBirthDate: e.target.value }))}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
          </Stack>

          <TextField
            select
            label={t('createUserModal.field.language')}
            value={form.fhirCommunicationLang}
            onChange={(e) => setForm((prev) => ({ ...prev, fhirCommunicationLang: e.target.value }))}
            fullWidth
          >
            <MenuItem value="">{t('createUserModal.field.language.unknown')}</MenuItem>
            <MenuItem value="pt-BR">{t('createUserModal.field.language.ptBR')}</MenuItem>
            <MenuItem value="es">{t('createUserModal.field.language.es')}</MenuItem>
            <MenuItem value="en">{t('createUserModal.field.language.en')}</MenuItem>
            <MenuItem value="fr">{t('createUserModal.field.language.fr')}</MenuItem>
          </TextField>

              {/* ── Contatos do profissional ───────────────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">{t('createUserModal.section.contacts')}</Typography>
              </Divider>

              <Box>
                <Stack spacing={1}>
                  {form.fhirTelecom.map((tc, idx) => (
                    <Stack key={idx} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
                      <TextField
                        select label={t('createUserModal.field.telecom.type')} value={tc.system} size="small"
                        onChange={(e) => {
                          const updated = [...form.fhirTelecom];
                          updated[idx] = { ...updated[idx], system: e.target.value as PractTelecomSystem };
                          setForm((prev) => ({ ...prev, fhirTelecom: updated }));
                        }}
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="phone">{t('createUserModal.field.telecom.system.phone')}</MenuItem>
                        <MenuItem value="fax">{t('createUserModal.field.telecom.system.fax')}</MenuItem>
                        <MenuItem value="email">{t('createUserModal.field.telecom.system.email')}</MenuItem>
                        <MenuItem value="url">{t('createUserModal.field.telecom.system.url')}</MenuItem>
                        <MenuItem value="sms">{t('createUserModal.field.telecom.system.sms')}</MenuItem>
                        <MenuItem value="other">{t('createUserModal.field.telecom.system.other')}</MenuItem>
                      </TextField>
                      <TextField
                        label={t('createUserModal.field.telecom.value')} value={tc.value} size="small" fullWidth
                        placeholder={tc.system === 'phone' ? '+55 11 99999-9999' : ''}
                        onChange={(e) => {
                          const updated = [...form.fhirTelecom];
                          updated[idx] = { ...updated[idx], value: e.target.value };
                          setForm((prev) => ({ ...prev, fhirTelecom: updated }));
                        }}
                      />
                      <TextField
                        select label={t('createUserModal.field.telecom.use')} value={tc.use} size="small"
                        onChange={(e) => {
                          const updated = [...form.fhirTelecom];
                          updated[idx] = { ...updated[idx], use: e.target.value as PractTelecomUse };
                          setForm((prev) => ({ ...prev, fhirTelecom: updated }));
                        }}
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="work">{t('createUserModal.field.telecom.use.work')}</MenuItem>
                        <MenuItem value="home">{t('createUserModal.field.telecom.use.home')}</MenuItem>
                        <MenuItem value="mobile">{t('createUserModal.field.telecom.use.mobile')}</MenuItem>
                        <MenuItem value="temp">{t('createUserModal.field.telecom.use.temp')}</MenuItem>
                        <MenuItem value="old">{t('createUserModal.field.telecom.use.old')}</MenuItem>
                      </TextField>
                      <IconButton size="small" color="error" sx={{ mt: 0.5 }}
                        onClick={() => setForm((prev) => ({ ...prev, fhirTelecom: prev.fhirTelecom.filter((_, i) => i !== idx) }))}
                        aria-label={t('createUserModal.field.telecom.remove')}
                      >
                        <RemoveCircleOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                  <Button size="small" startIcon={<AddCircleOutlineIcon />} sx={{ alignSelf: 'flex-start' }}
                    onClick={() => setForm((prev) => ({ ...prev, fhirTelecom: [...prev.fhirTelecom, { ...EMPTY_PRACT_TELECOM }] }))}
                  >
                    {t('createUserModal.field.telecom.add')}
                  </Button>
                </Stack>
              </Box>

              {/* ── Endereço residencial ───────────────────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">{t('createUserModal.section.address')}</Typography>
              </Divider>

              <Stack spacing={1.5}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label={t('createUserModal.field.address.street')}
                    value={form.fhirAddress.line}
                    onChange={(e) => setForm((prev) => ({ ...prev, fhirAddress: { ...prev.fhirAddress, line: e.target.value } }))}
                    size="small" fullWidth placeholder="Rua das Flores, 42"
                  />
                  <TextField
                    label={t('createUserModal.field.address.complement')}
                    value={form.fhirAddress.complement}
                    onChange={(e) => setForm((prev) => ({ ...prev, fhirAddress: { ...prev.fhirAddress, complement: e.target.value } }))}
                    size="small" fullWidth placeholder="Apto 12, Bela Vista"
                  />
                </Stack>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label={t('createUserModal.field.address.city')}
                    value={form.fhirAddress.city}
                    onChange={(e) => setForm((prev) => ({ ...prev, fhirAddress: { ...prev.fhirAddress, city: e.target.value } }))}
                    size="small" fullWidth placeholder="São Paulo"
                  />
                  <TextField
                    label={t('createUserModal.field.address.state')}
                    value={form.fhirAddress.state}
                    onChange={(e) => setForm((prev) => ({ ...prev, fhirAddress: { ...prev.fhirAddress, state: e.target.value } }))}
                    size="small" inputProps={{ maxLength: 2 }} placeholder="SP"
                    sx={{ minWidth: 80, maxWidth: 80 }}
                  />
                  <TextField
                    label={t('createUserModal.field.address.postalCode')}
                    value={form.fhirAddress.postalCode}
                    onChange={(e) => setForm((prev) => ({ ...prev, fhirAddress: { ...prev.fhirAddress, postalCode: e.target.value } }))}
                    size="small" inputProps={{ maxLength: 9 }} placeholder="01310-100"
                    InputProps={{ endAdornment: <InputAdornment position="end">BR</InputAdornment> }}
                    sx={{ minWidth: 150 }}
                  />
                </Stack>
              </Stack>

              {/* ── Formação / Registros profissionais ─────────────────── */}
              <Divider sx={{ my: 1 }}>
                <Typography variant="caption" color="text.secondary">{t('createUserModal.section.qualifications')}</Typography>
              </Divider>

              <Box>
                <Stack spacing={1}>
                  {form.fhirQualifications.map((q, idx) => (
                    <Stack key={idx} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
                      <TextField
                        select label={t('createUserModal.field.qualification.council')} value={q.system} size="small"
                        onChange={(e) => {
                          const updated = [...form.fhirQualifications];
                          updated[idx] = { ...updated[idx], system: e.target.value };
                          setForm((prev) => ({ ...prev, fhirQualifications: updated }));
                        }}
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="CRM">CRM</MenuItem>
                        <MenuItem value="CRO">CRO</MenuItem>
                        <MenuItem value="COREN">COREN</MenuItem>
                        <MenuItem value="CRF">CRF</MenuItem>
                        <MenuItem value="CRP">CRP / CFP</MenuItem>
                        <MenuItem value="OUTRO">Outro</MenuItem>
                      </TextField>
                      <TextField
                        label={t('createUserModal.field.qualification.code')} value={q.code} size="small"
                        placeholder="123456/SP"
                        onChange={(e) => {
                          const updated = [...form.fhirQualifications];
                          updated[idx] = { ...updated[idx], code: e.target.value };
                          setForm((prev) => ({ ...prev, fhirQualifications: updated }));
                        }}
                        sx={{ minWidth: 160 }}
                      />
                      <TextField
                        label={t('createUserModal.field.qualification.display')} value={q.display} size="small" fullWidth
                        placeholder="Clínico Geral, Cirurgiã-Dentista..."
                        onChange={(e) => {
                          const updated = [...form.fhirQualifications];
                          updated[idx] = { ...updated[idx], display: e.target.value };
                          setForm((prev) => ({ ...prev, fhirQualifications: updated }));
                        }}
                      />
                      <IconButton size="small" color="error" sx={{ mt: 0.5 }}
                        onClick={() => setForm((prev) => ({ ...prev, fhirQualifications: prev.fhirQualifications.filter((_, i) => i !== idx) }))}
                        aria-label={t('createUserModal.field.qualification.remove')}
                      >
                        <RemoveCircleOutlineIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                  <Button size="small" startIcon={<AddCircleOutlineIcon />} sx={{ alignSelf: 'flex-start' }}
                    onClick={() => setForm((prev) => ({ ...prev, fhirQualifications: [...prev.fhirQualifications, { ...EMPTY_QUALIFICATION }] }))}
                  >
                    {t('createUserModal.field.qualification.add')}
                  </Button>
                </Stack>
              </Box>
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose} disabled={loading}>
            {t('createUserModal.actions.cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={loading || (!isEditMode && (!selectedLocationId || !selectedRoleCode))}
            data-testid="btn-submit-user"
            startIcon={loading ? <CircularProgress size={16} /> : null}
          >
            {loading
              ? t('createUserModal.actions.saving')
              : isEditMode
                ? t('createUserModal.actions.save')
                : t('createUserModal.actions.create')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
