/**
 * T141 [P] [US2] E2E test: super-user creates tenant via corrected form.
 *
 * Verifies:
 * 1. All required fields (CNES, adminDisplayName, adminEmail, adminCPF,
 *    adminPassword, organizationDisplayName) are rendered.
 * 2. Submit calls POST /api/admin/tenants (not /tenants/create).
 * 3. Success (201) shows Toast/Alert confirmation.
 * 4. CNES conflict (409) shows OperationOutcome error feedback.
 * 5. Form is hidden/disabled for profile 10/20 (RbacPermissionGuard active).
 *
 * TDD state: RED until T136, T138, T139 are implemented.
 *
 * Refs: FR-003, FR-022
 */

import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const SUPER_SESSION = {
  sessionId: 'session-super-001',
  expiresAt: '2099-12-31T23:59:59Z',
  practitioner: {
    id: 'prac-super-001',
    email: 'super@clinicadigital.local',
    profileType: 0,
    displayName: 'Super Admin',
    accountActive: true,
    identifiers: [],
    names: [{ text: 'Super Admin' }],
  },
  tenant: {
    id: 'uuid-tenant-super',
    name: 'clinicadigital',
    displayName: 'Clínica Digital',
    cnes: '0000000',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

const PROFILE_20_SESSION = {
  ...SUPER_SESSION,
  sessionId: 'session-profile20-001',
  practitioner: {
    ...SUPER_SESSION.practitioner,
    id: 'prac-profile20-001',
    email: 'medico@clinica.local',
    profileType: 20,
    displayName: 'Dr. Medico',
  },
};

const TENANT_LIST_RESPONSE = [
  {
    tenantId: 'uuid-tenant-001',
    organization: {
      displayName: 'Clínica Aurora',
      cnes: '1234567',
      active: true,
    },
  },
];

const CREATE_SUCCESS_RESPONSE = {
  tenantId: 'uuid-new-tenant',
  adminPractitionerId: 'uuid-new-prac',
  organization: {
    displayName: 'Clínica Horizonte',
    cnes: '7654321',
    active: true,
  },
  practitioner: {
    id: 'uuid-new-prac',
    email: 'admin@horizonte.local',
    profileType: 0,
    displayName: 'Admin Horizonte',
    active: true,
  },
};

const CNES_CONFLICT_RESPONSE = {
  resourceType: 'OperationOutcome',
  issue: [
    {
      severity: 'error',
      code: 'conflict',
      details: { text: 'CNES já cadastrado para outro tenant' },
      diagnostics: 'CNES 7654321 already registered',
    },
  ],
};

// ---------------------------------------------------------------------------
// Helper: authenticate as super-user (profile 0)
// ---------------------------------------------------------------------------

async function loginAsSuperUser(page: import('@playwright/test').Page) {
  await page.route('**/api/auth/login', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ mode: 'single', session: SUPER_SESSION }),
    });
  });
  await page.route('**/api/admin/tenants', (route) => {
    if (route.request().method() === 'GET') {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(TENANT_LIST_RESPONSE),
      });
    } else {
      route.fallback();
    }
  });
  await page.goto('/login');
  await page.fill('[name="email"], [type="email"]', 'super@clinicadigital.local');
  await page.fill('[name="password"], [type="password"]', 'senha123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin/tenants', { timeout: 5000 });
}

// ---------------------------------------------------------------------------
// Test Suite
// ---------------------------------------------------------------------------

test.describe('TenantAdmin — formulário corrigido (super-user)', () => {
  test('renderiza todos os campos obrigatórios do formulário', async ({ page }) => {
    await loginAsSuperUser(page);

    // Required form fields
    await expect(
      page.getByLabel(/nome da organização|organization.*name|displayName/i).or(
        page.locator('[name="organizationDisplayName"], [aria-label*="organiza" i]')
      )
    ).toBeVisible({ timeout: 5000 });

    await expect(
      page.getByLabel(/cnes/i).or(page.locator('[name="cnes"]'))
    ).toBeVisible({ timeout: 5000 });

    await expect(
      page.getByLabel(/nome do admin|admin.*nome|adminDisplayName/i).or(
        page.locator('[name="adminDisplayName"], [aria-label*="admin" i][aria-label*="nome" i]')
      )
    ).toBeVisible({ timeout: 5000 });

    await expect(
      page.getByLabel(/e-?mail.*admin|admin.*e-?mail/i).or(
        page.locator('[name="adminEmail"]')
      )
    ).toBeVisible({ timeout: 5000 });

    await expect(
      page.getByLabel(/cpf/i).or(page.locator('[name="adminCpf"]'))
    ).toBeVisible({ timeout: 5000 });

    await expect(
      page.getByLabel(/senha.*admin|admin.*senha|password/i).or(
        page.locator('[name="adminPassword"]')
      )
    ).toBeVisible({ timeout: 5000 });
  });

  test('submit chama POST /api/admin/tenants com payload correto', async ({ page }) => {
    let capturedBody: unknown = null;

    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ mode: 'single', session: SUPER_SESSION }),
      });
    });
    await page.route('**/api/admin/tenants', (route) => {
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(TENANT_LIST_RESPONSE),
        });
      } else if (route.request().method() === 'POST') {
        capturedBody = JSON.parse(route.request().postData() ?? '{}');
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(CREATE_SUCCESS_RESPONSE),
        });
      } else {
        route.fallback();
      }
    });

    await page.goto('/login');
    await page.fill('[name="email"], [type="email"]', 'super@clinicadigital.local');
    await page.fill('[name="password"], [type="password"]', 'senha123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/tenants', { timeout: 5000 });

    // Fill required fields
    await page.fill('[name="organizationDisplayName"], [aria-label*="nome da organização" i]', 'Clínica Horizonte');
    await page.fill('[name="cnes"]', '7654321');
    await page.fill('[name="adminDisplayName"]', 'Admin Horizonte');
    await page.fill('[name="adminEmail"], [type="email"][name*="admin" i]', 'admin@horizonte.local');
    await page.fill('[name="adminCpf"]', '12345678901');
    await page.fill('[name="adminPassword"], [type="password"][name*="admin" i]', 'S3nha@Admin123');

    await page.click('button[type="submit"][data-testid="btn-criar-tenant"], button[type="submit"]:has-text("Criar")');

    await expect(
      page.getByRole('alert').or(
        page.getByText(/criado|sucesso/i).or(page.getByTestId('toast-success'))
      )
    ).toBeVisible({ timeout: 5000 });

    expect(capturedBody).toMatchObject({
      organization: { displayName: 'Clínica Horizonte', cnes: '7654321' },
      adminPractitioner: {
        displayName: 'Admin Horizonte',
        email: 'admin@horizonte.local',
        cpf: '12345678901',
        password: 'S3nha@Admin123',
      },
    });
  });

  test('CNES duplicado exibe feedback de conflito via OperationOutcome', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ mode: 'single', session: SUPER_SESSION }),
      });
    });
    await page.route('**/api/admin/tenants', (route) => {
      if (route.request().method() === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
      } else {
        route.fulfill({
          status: 409,
          contentType: 'application/fhir+json',
          body: JSON.stringify(CNES_CONFLICT_RESPONSE),
        });
      }
    });

    await page.goto('/login');
    await page.fill('[name="email"], [type="email"]', 'super@clinicadigital.local');
    await page.fill('[name="password"], [type="password"]', 'senha123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/tenants', { timeout: 5000 });

    await page.fill('[name="organizationDisplayName"], [aria-label*="nome da organização" i]', 'Clínica Duplicada');
    await page.fill('[name="cnes"]', '7654321');
    await page.fill('[name="adminDisplayName"]', 'Admin Duplicado');
    await page.fill('[name="adminEmail"], [type="email"][name*="admin" i]', 'dup@dup.local');
    await page.fill('[name="adminCpf"]', '99999999999');
    await page.fill('[name="adminPassword"], [type="password"][name*="admin" i]', 'Senha@Dup123');

    await page.click('button[type="submit"]');

    await expect(
      page.getByRole('alert').or(
        page.getByText(/cnes.*cadastrado|conflito|conflict/i)
      )
    ).toBeVisible({ timeout: 5000 });
  });

  test('formulário não é visível para profile 10/20 (RbacPermissionGuard)', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ mode: 'single', session: PROFILE_20_SESSION }),
      });
    });
    await page.route('**/api/admin/tenants', (route) => {
      if (route.request().method() === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
      } else {
        route.fallback();
      }
    });

    await page.goto('/login');
    await page.fill('[name="email"], [type="email"]', 'medico@clinica.local');
    await page.fill('[name="password"], [type="password"]', 'senha123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/admin/tenants', { timeout: 5000 });

    // Form should NOT be visible
    await expect(
      page.locator('[data-testid="tenant-create-form"]')
    ).not.toBeVisible({ timeout: 3000 });

    // Access denied message should appear
    await expect(
      page.getByText(/super-usuário|permissão|acesso negado/i).or(
        page.getByRole('alert')
      )
    ).toBeVisible({ timeout: 3000 });
  });
});
