import Stack from '@mui/material/Stack';
import { useState } from 'react';
import { LoginForm } from '../components/organisms/LoginForm';
import { SessionHistory } from '../components/organisms/SessionHistory';
import { TenantAdmin } from '../components/organisms/TenantAdmin';
import { AuthTemplate } from '../components/templates/AuthTemplate';
import { MainTemplate } from '../components/templates/MainTemplate';
import { getDefaultNavigationSchema } from '../services/navigationSchema';

type Tenant = {
  id: string;
  legalName: string;
  slug: string;
  status: 'active' | 'suspended' | 'blocked';
  planTier: string;
};

type SessionItem = {
  id: string;
  tenantSlug: string;
  user: string;
  outcome: 'success' | 'failure';
  traceId: string;
};

const initialTenants: Tenant[] = [
  {
    id: 't-1',
    legalName: 'Clinica Aurora',
    slug: 'aurora',
    status: 'active',
    planTier: 'enterprise'
  },
  {
    id: 't-2',
    legalName: 'Clinica Horizonte',
    slug: 'horizonte',
    status: 'suspended',
    planTier: 'growth'
  }
];

const initialSessions: SessionItem[] = [
  {
    id: 's-1',
    tenantSlug: 'aurora',
    user: 'admin@aurora.com',
    outcome: 'success',
    traceId: 'trace-aurora-001'
  },
  {
    id: 's-2',
    tenantSlug: 'horizonte',
    user: 'sec@horizonte.com',
    outcome: 'failure',
    traceId: 'trace-horizonte-009'
  }
];

export default function App() {
  const [loggedIn, setLoggedIn] = useState(false);
  const [tenants, setTenants] = useState(initialTenants);
  const navigationSchema = getDefaultNavigationSchema('tenant-clinica-digital');

  if (!loggedIn) {
    return (
      <AuthTemplate>
        <LoginForm
          onLogin={async () => {
            setLoggedIn(true);
          }}
        />
      </AuthTemplate>
    );
  }

  return (
    <MainTemplate
      trainingContext={{
        tenant_id: 'tenant-clinica-digital',
        trace_id: 'trace-shell-001',
        user_id: 'admin@clinica.local',
        role: ['support']
      }}
      navigationSchema={navigationSchema}
    >
      <Stack spacing={3}>
        <h1>Painel Operacional Multi-tenant</h1>
        <TenantAdmin
          tenants={tenants}
          onCreateTenant={(tenantName) => {
            const slug = tenantName.toLowerCase().replace(/\s+/g, '-');
            setTenants((current) => [
              ...current,
              {
                id: `t-${current.length + 1}`,
                legalName: tenantName,
                slug,
                status: 'active',
                planTier: 'starter'
              }
            ]);
          }}
        />
        <SessionHistory sessions={initialSessions} />
      </Stack>
    </MainTemplate>
  );
}
