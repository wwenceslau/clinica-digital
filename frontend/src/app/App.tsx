/**
 * T075 [US5] App entrypoint with AuthProvider + TenantProvider injection.
 * T118 [US10] Added /login and /register public routes without altering provider order.
 *
 * Provider hierarchy (unchanged from US5):
 *   AuthProvider → TenantProvider → Routes
 *
 * Public routes (/login, /register) render outside ProtectedRoute.
 * All other routes are wrapped in ProtectedRoute, which shows the login view
 * when unauthenticated or redirects back to the shell when already authenticated.
 *
 * T098: BrowserRouter + Routes added to support /admin/usuarios.
 *
 * Refs: FR-012, FR-013
 */

import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import { BrowserRouter, Route, Routes, Navigate } from 'react-router-dom';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { AuthProvider } from '../context/AuthContext';
import { TenantProvider } from '../context/TenantContext';
import { LocaleContextProvider } from '../context/LocaleContext';
import { ThemeContextProvider } from '../context/ThemeContext';
import { SessionHistory } from '../components/organisms/SessionHistory';
import { TenantAdmin } from '../components/organisms/TenantAdmin';
import type { Tenant } from '../components/organisms/TenantAdmin';
import { MainTemplate } from '../components/templates/MainTemplate';
import { AuthTemplate } from '../components/templates/AuthTemplate';
import { LoginForm } from '../components/organisms/LoginForm';
import { ClinicRegistrationForm } from '../components/organisms/ClinicRegistrationForm';
import { listTenants, createTenantApi, updateTenantApi, deleteTenantApi } from '../services/tenantApi';
import type { CreateTenantRequest } from '../services/tenantApi';
import { fromCaughtError } from '../services/operationOutcomeAdapter';
import { ProtectedRoute } from './ProtectedRoute';
import { AdminUsuariosPage } from './AdminUsuariosPage';
import { SecurityUsersPage } from './SecurityUsersPage';
import { SecurityRolesPage } from './SecurityRolesPage';
import { SecurityAuditPage } from './SecurityAuditPage';
import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { RbacPermissionGuard } from '../components/atoms/RbacPermissionGuard';
import type { SessionIssuedResponse } from '../services/iamAuthApi';

const muiTheme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      dark: '#115293',
      contrastText: '#fff',
    },
    background: {
      default: '#f4f7f6',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h6: { fontWeight: 700 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiAppBar: { styleOverrides: { root: { borderRadius: 0 } } },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          margin: '2px 8px',
          '&:hover': { backgroundColor: '#e3f2fd' },
        },
      },
    },
  },
});

/**
 * Standalone /login route — renders login form in AuthTemplate.
 * When already authenticated, redirects to the shell root.
 */
function LoginPage() {
  const { isAuthenticated, login } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  async function handleLogin(session: SessionIssuedResponse) {
    login(session);
  }

  return (
    <AuthTemplate>
      <LoginForm onLogin={handleLogin} />
    </AuthTemplate>
  );
}

/**
 * Standalone /register route — renders the clinic registration form in AuthTemplate.
 */
function RegisterPage() {
  return (
    <AuthTemplate>
      <ClinicRegistrationForm onSuccess={() => {/* redirect handled inside form on success */}} />
    </AuthTemplate>
  );
}


type SessionItem = {
  id: string;
  tenantSlug: string;
  user: string;
  outcome: 'success' | 'failure';
  traceId: string;
};

/**
 * T137 [US5] Wrapper que injeta MainTemplate com contexto real de autenticação.
 *
 * trainingContext é derivado da sessão autenticada via useAuth() + useTenant().
 * Retorna null (fallback) quando session ainda não carregou para evitar
 * piscar valores hardcoded. Resolve T075 (C4 — trainingContext hardcoded).
 *
 * Refs: FR-007, FR-012, FR-014
 */
export function ShellPage({ children }: { children: React.ReactNode }) {
  const { session } = useAuth();
  const traceIdRef = useRef<string>(crypto.randomUUID());

  if (!session) {
    return null;
  }

  return (
    <MainTemplate
      trainingContext={{
        tenant_id: session.tenant.id,
        trace_id: traceIdRef.current,
        user_id: session.practitioner.id,
        role: [String(session.practitioner.profileType)],
      }}
    >
      {children}
    </MainTemplate>
  );
}

function TenantAdminPage() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const refreshTenants = () => {
    setLoading(true);
    setErrorMessage(null);
    listTenants()
      .then((data) =>
        setTenants(
          data.map((t) => ({
            id: t.id,
            legalName: t.legalName,
            slug: t.slug,
            status: t.status as Tenant['status'],
            planTier: t.planTier,
            adminDisplayName: t.adminDisplayName,
            adminEmail: t.adminEmail,
            adminCpf: t.adminCpf,
          })),
        ),
      )
      .catch((err) => {
        const outcome = fromCaughtError(err);
        setErrorMessage(outcome.userMessage);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    refreshTenants();
  }, []);

  return (
    <ShellPage>
      <Stack spacing={3}>
        {errorMessage && (
          <Alert severity="error" onClose={() => setErrorMessage(null)}>
            {errorMessage}
          </Alert>
        )}
        <RbacPermissionGuard permission="iam.tenant.create">
          <TenantAdmin
            tenants={tenants}
            loading={loading}
            onRefresh={refreshTenants}
            onCreateTenant={(payload: CreateTenantRequest) => {
              setLoading(true);
              setErrorMessage(null);
              createTenantApi(payload)
                .then((created) => {
                  setTenants((cur) => [
                    ...cur,
                    {
                      id: created.tenantId,
                      legalName: created.organization.displayName,
                      slug: created.organization.cnes,
                      status: 'active' as Tenant['status'],
                      planTier: 'starter',
                      adminDisplayName: payload.adminPractitioner.displayName,
                      adminEmail: payload.adminPractitioner.email,
                      adminCpf: payload.adminPractitioner.cpf,
                    },
                  ]);
                })
                .catch((err) => {
                  const outcome = fromCaughtError(err);
                  setErrorMessage(outcome.userMessage);
                })
                .finally(() => setLoading(false));
            }}
            onUpdateTenant={(tenantId, payload) => {
              const existing = tenants.find((t) => t.id === tenantId);
              if (!existing) return;
              setLoading(true);
              setErrorMessage(null);
              updateTenantApi(tenantId, {
                organization: payload.organization,
                adminPractitioner: payload.adminPractitioner,
                planTier: existing.planTier,
              })
                .then((updated) => {
                  setTenants((cur) =>
                    cur.map((t) =>
                      t.id === tenantId
                        ? {
                            ...t,
                            legalName: updated.legalName,
                            slug: updated.slug,
                            planTier: updated.planTier,
                            status: updated.status as Tenant['status'],
                            adminDisplayName: updated.adminDisplayName,
                            adminEmail: updated.adminEmail,
                            adminCpf: updated.adminCpf,
                          }
                        : t,
                    ),
                  );
                })
                .catch((err) => {
                  const outcome = fromCaughtError(err);
                  setErrorMessage(outcome.userMessage);
                })
                .finally(() => setLoading(false));
            }}
            onDeleteTenant={(tenantId) => {
              setLoading(true);
              setErrorMessage(null);
              deleteTenantApi(tenantId)
                .then(() => {
                  setTenants((cur) => cur.filter((t) => t.id !== tenantId));
                })
                .catch((err) => {
                  const outcome = fromCaughtError(err);
                  setErrorMessage(outcome.userMessage);
                })
                .finally(() => setLoading(false));
            }}
          />
        </RbacPermissionGuard>
        <SessionHistory sessions={[] as SessionItem[]} />
      </Stack>
    </ShellPage>
  );
}

export default function App() {
  return (
    <ThemeProvider theme={muiTheme}>
      <ThemeContextProvider>
        <LocaleContextProvider>
          <BrowserRouter>
            <AuthProvider>
              <TenantProvider>
                <Routes>
                  {/* Public */}
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />

                  {/* Protected */}
                  <Route
                    path="*"
                    element={
                      <ProtectedRoute>
                        <Routes>
                          <Route path="/admin/tenants" element={<TenantAdminPage />} />
                          <Route path="/admin/usuarios" element={<ShellPage><AdminUsuariosPage /></ShellPage>} />
                          <Route path="/admin/security/users" element={<ShellPage><SecurityUsersPage /></ShellPage>} />
                          <Route path="/admin/security/roles" element={<ShellPage><SecurityRolesPage /></ShellPage>} />
                          <Route path="/admin/security/audit" element={<ShellPage><SecurityAuditPage /></ShellPage>} />
                          {/* Default shell — redirects to /admin/tenants */}
                          <Route path="*" element={<Navigate to="/admin/tenants" replace />} />
                        </Routes>
                      </ProtectedRoute>
                    }
                  />
                </Routes>
              </TenantProvider>
            </AuthProvider>
          </BrowserRouter>
        </LocaleContextProvider>
      </ThemeContextProvider>
    </ThemeProvider>
  );
}
