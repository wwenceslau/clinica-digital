# Quick Start: Integrating the Shell

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-10  
**Audience**: Frontend developers integrating Shell into authenticated routes

## Overview

This document guides developers on how to integrate the Shell (`MainTemplate`) into the Clínica Digital application and set up the required context providers and configuration.

## Prerequisites

- Node.js 18+ with npm 9+
- React 19 + TypeScript strict mode
- Existing frontend project using Vite, MUI 7, and Tailwind CSS
- Backend authentication service (JWT-based) already deployed
- React Router v6+ configured

## Installation & Setup

### 1. Environment Configuration

Create or update `.env.local`:

```bash
# Theme default (can be overridden at runtime)
VITE_SHELL_DEFAULT_THEME=light

# Locale default
VITE_SHELL_DEFAULT_LOCALE=pt-BR

# Debug mode (enable telemetry footer)
VITE_DEBUG_MODE=false

# API endpoint for navigation schema
VITE_API_SHELL_NAVIGATION_SCHEMA=https://api.clinica-digital/api/shell/navigation-schema
```

### 2. CSS Token Setup

Ensure `frontend/src/index.css` includes the globals.css pattern with dual-theme tokens:

```css
/* Add to index.css or globals.css */

:root[data-theme="light"] {
  --shell-color-primary: #1976d2;
  --shell-color-secondary: #dc004e;
  --shell-color-text: #000;
  --shell-color-text-secondary: #666;
  --shell-spacing-sm: 8px;
  --shell-spacing-md: 16px;
  --shell-spacing-lg: 24px;
  /* ... (see data-model.md for full token list) */
}

:root[data-theme="dark"] {
  --shell-color-primary: #90caf9;
  --shell-color-secondary: #f48fb1;
  --shell-color-text: #fff;
  --shell-color-text-secondary: #bbb;
  /* ... (see data-model.md for dark theme tokens) */
}
```

### 3. i18n Configuration

Install and configure `react-i18next`:

```bash
npm install i18next react-i18next
```

Create `frontend/src/i18n/config.ts`:

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Placeholder  namespace (translations deferred to implementation)
const resources = {
  'pt-BR': {
    translation: {
      'sidebar.domain.administration': 'Administração',
      'sidebar.domain.professionals': 'Profissionais',
      'sidebar.domain.patients': 'Pacientes',
      'sidebar.domain.scheduling': 'Agenda',
      'sidebar.domain.clinical-care': 'Atendimento',
      'sidebar.domain.diagnostics-therapy': 'Diagnóstico & Terapêutica',
      'sidebar.domain.prevention': 'Prevenção',
      'sidebar.domain.billing': 'Financeiro & Faturamento',
      'sidebar.domain.security': 'Segurança',
      'header.tenant': 'Clínica',
      'header.location': 'Unidade',
      'header.profile': 'Perfil',
      'telemetry.trace-id': 'Trace ID',
      'telemetry.tenant-id': 'Tenant ID',
      // ... (complete namespace keys from data-model.md)
    },
  },
  'en-US': {
    translation: {
      'sidebar.domain.administration': 'Administration',
      'sidebar.domain.professionals': 'Professionals',
      'sidebar.domain.patients': 'Patients',
      'sidebar.domain.scheduling': 'Scheduling',
      'sidebar.domain.clinical-care': 'Clinical Care',
      'sidebar.domain.diagnostics-therapy': 'Diagnostics & Therapy',
      'sidebar.domain.prevention': 'Prevention',
      'sidebar.domain.billing': 'Billing & Billing',
      'sidebar.domain.security': 'Security',
      'header.tenant': 'Clinic',
      'header.location': 'Unit',
      'header.profile': 'Profile',
      'telemetry.trace-id': 'Trace ID',
      'telemetry.tenant-id': 'Tenant ID',
      // ... (complete namespace keys)
    },
  },
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: 'pt-BR',
    fallbackLng: 'pt-BR',
    interpolation: {
      escapeValue: false, // React already escapes by default
    },
  });

export default i18n;
```

### 4. Application Entry Point

Update your React app entry point (`frontend/src/main.tsx`):

```typescript
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './app/App';
import i18n from './i18n/config';
import './index.css'; // Ensure globals.css is imported

// Initialize i18n
i18n;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

### 5. App Routing Structure

Update your main App component (`frontend/src/app/App.tsx`):

```typescript
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { I18nextProvider } from 'react-i18next';
import i18n from '../i18n/config';
import LoginPage from './pages/LoginPage';
import MainTemplate from '../components/templates/MainTemplate';
import DashboardPage from './pages/DashboardPage';
import PatientsPage from './pages/PatientsPage';
import SecurityUsersPage from './pages/security/SecurityUsersPage';

export default function App() {
  return (
    <I18nextProvider i18n={i18n}>
      <Router>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected routes with Shell */}
          <Route
            path="/app/*"
            element={
              <MainTemplate
                initialTheme="light"
                initialLocale="pt-BR"
                trainingContext={{
                  tenant_id: 'tenant-001',     // From JWT
                  trace_id: 'trace-uuid',       // From request headers
                  user_id: 'user-123',          // From JWT
                  role: ['medical-staff', 'admin'],
                }}
              >
                <Routes>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/patients/*" element={<PatientsPage />} />
                  <Route path="/admin/security/users" element={<SecurityUsersPage />} />
                  {/* ... other authenticated routes */}
                  <Route path="*" element={<Navigate to="/app" replace />} />
                </Routes>
              </MainTemplate>
            }
          />

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </I18nextProvider>
  );
}
```

## MainTemplate Props

The `MainTemplate` component accepts:

```typescript
interface MainTemplateProps {
  children: React.ReactNode;

  // Optional: Theme default (can be overridden by React Context)
  initialTheme?: 'light' | 'dark';

  // Optional: Locale default (can be overridden by React Context)
  initialLocale?: 'pt-BR' | 'en-US';

  // Required: Authentication/Tenant context passed from App
  trainingContext: {
    tenant_id: string;     // UUID of the clinic/tenant
    trace_id: string;      // UUID for request tracing
    user_id: string;       // UUID of authenticated user
    role: string[];        // Array of clinical roles (e.g., ['physician', 'admin'])
  };
}
```

## Using Shell Context in Child Components

Child components within `MainTemplate` can access Shell context via React hooks:

```typescript
import { useContext } from 'react';
import { ShellContext } from '../context/ShellContext';
import { useTranslation } from 'react-i18next';

export function MyPage() {
  const { tenant_id, active_location_id, trace_id } = useContext(ShellContext);
  const { t } = useTranslation();

  return (
    <div>
      <h1>{t('pages.my-page.title')}</h1>
      <p>Operating in location: {active_location_id}</p>
      <p>Trace ID: {trace_id}</p>
    </div>
  );
}
```

## Navigation Schema API Contract

The backend must provide a `/api/shell/navigation-schema` endpoint that returns the domain + resources structure:

**Request**:
```
GET /api/shell/navigation-schema
Headers:
  Authorization: Bearer {jwt_token}
  X-Tenant-ID: {tenant_id}
  X-Trace-ID: {trace_id}
```

**Response** (200 OK):
```json
{
  "version": "1.0.0",
  "tenant_id": "{tenant_id}",
  "domains": [
    {
      "domain_id": "seguranca",
      "domain_label_key": "sidebar.domain.security",
      "resources": [
        {
          "resource_id": "internal-user-management",
          "label_key": "sidebar.security.user-management",
          "icon_id": "people",
          "route": "/app/admin/security/users",
          "permission_key": "perm.security:read:user-management"
        }
        // ... more resources
      ]
    }
    // ... other domains
  ]
}
```

## Testing the Shell

### Unit Tests

Example unit test for MainTemplate:

```typescript
// frontend/src/components/templates/MainTemplate.test.tsx
import { render, screen } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import i18n from '../../i18n/config';
import MainTemplate from './MainTemplate';

describe('MainTemplate', () => {
  it('renders Header, Sidebar, and children', () => {
    render(
      <I18nextProvider i18n={i18n}>
        <MainTemplate
          initialTheme="light"
          initialLocale="pt-BR"
          trainingContext={{
            tenant_id: 'tenant-001',
            trace_id: 'trace-123',
            user_id: 'user-456',
            role: ['physician'],
          }}
        >
          <div data-testid="child-content">Child Page</div>
        </MainTemplate>
      </I18nextProvider>
    );

    expect(screen.getByTestId('shell-header')).toBeInTheDocument();
    expect(screen.getByTestId('shell-sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('child-content')).toBeInTheDocument();
  });

  it('respects initialTheme prop', () => {
    const { container } = render(
      <I18nextProvider i18n={i18n}>
        <MainTemplate
          initialTheme="dark"
          initialLocale="pt-BR"
          trainingContext={{
            tenant_id: 'tenant-001',
            trace_id: 'trace-123',
            user_id: 'user-456',
            role: ['physician'],
          }}
        >
          Content
        </MainTemplate>
      </I18nextProvider>
    );

    const mainTemplate = container.querySelector('[data-theme="dark"]');
    expect(mainTemplate).toBeInTheDocument();
  });
});
```

### E2E Tests (Playwright)

Example e2e test for Shell navigation:

```typescript
// frontend/e2e/shell-navigation.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Shell Navigation', () => {
  test.beforeEach(async ({ page }) => {
    // Login and navigate to authenticated area
    await page.goto('/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'testpass');
    await page.click('button[type="submit"]');
    await page.waitForURL('/app/**');
  });

  test('displays all domain groups in Sidebar', async ({ page }) => {
    const sidebar = page.locator('[data-testid="shell-sidebar"]');
    expect(sidebar).toBeVisible();

    const domains = page.locator('[data-testid="sidebar-domain-group"]');
    expect(await domains.count()).toBe(9); // All 9 domains
  });

  test('navigates to resource when item clicked', async ({ page }) => {
    await page.click('[data-testid="sidebar-item-patient-search"]');
    await page.waitForURL('/app/patients/search');
    expect(page.url()).toContain('/app/patients/search');
  });

  test('displays tenant and location in Header', async ({ page }) => {
    const header = page.locator('[data-testid="shell-header"]');
    expect(header).toContainText('Clínica Digital'); // Tenant name
    expect(header).toContainText('Unidade 01');     // Location name
  });

  test('header LCP target is met', async ({ page }) => {
    const metrics = await page.evaluate(() => {
      const navigationTiming = window.performance.getEntriesByType('navigation')[0];
      return {
        lcp: Math.max(...window.performance.getEntriesByType('largest-contentful-paint').map((e) => e.startTime)),
      };
    });

    expect(metrics.lcp).toBeLessThan(1500); // LCP ≤ 1.5s
  });
});
```

## Accessibility Validation

Run automated accessibility checks:

```bash
# Install axe-core for accessibility audits
npm install -D @axe-core/react

# Run accessibility audit in tests
npm run test:a11y

# Or use Playwright with accessibility checks
npm run e2e:a11y
```

## Performance Monitoring

Monitor Shell performance in production:

```typescript
// frontend/src/services/observability.ts
import { performance } from 'perf_hooks';

export function logShellMetrics(trace_id: string, tenant_id: string) {
  const metrics = performance.getEntriesByType('navigation')[0];
  const lcp = Math.max(...performance.getEntriesByType('largest-contentful-paint').map((e) => e.startTime));

  const telemetry = {
    trace_id,
    tenant_id,
    event: 'shell.render',
    duration_ms: Math.round(lcp),
    timestamp: new Date().toISOString(),
  };

  // Send to telemetry backend
  fetch('/api/telemetry/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(telemetry),
  });
}
```

## Troubleshooting

### Shell not rendering

- Verify `MainTemplate` is wrapped by `I18nextProvider`
- Check that `trainingContext` is correctly passed with all required fields
- Ensure `initialTheme` is either 'light' or 'dark' (exact case)

### Translations not appearing

- Verify i18n config is initialized and keys exist in resources
- Check browser console for missing translation warnings
- Ensure locale is set to 'pt-BR' or 'en-US' (match keys in config)

### Styles not applied (dual-theme)

- Verify `globals.css` is imported in `index.css`
- Check that CSS variables are defined for `[data-theme="light"]` and `[data-theme="dark"]`
- Review browser DevTools to confirm `data-theme` attribute is set on root element

### Navigation items disabled or hidden

- Verify backend navigation schema returns correct `permitted` flag for each resource
- Check browser console for authorization errors
- Ensure user role in `trainingContext` matches backend permission checks

## Next Steps

1. **Implement Components** → Create atoms/, molecules/, organisms/ as defined in data-model.md
2. **Create Contract Tests** → Validate component props + API contracts
3. **Performance Tuning** → Run Lighthouse audit and optimize based on architecture spike results
4. **Localization** → Complete i18n resource files with full translations
5. **Integration Testing** → Test Shell in context of full authenticated flow
