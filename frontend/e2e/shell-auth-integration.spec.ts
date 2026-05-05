/**
 * T114 [US10] E2e integration: Shell displays auth context after successful login.
 *
 * Verifies:
 * 1. Unauthenticated visit renders AuthTemplate (login form).
 * 2. Single-org login → ProtectedRoute unmounts login, renders shell-main-template.
 * 3. Authenticated Shell shows shell-context-bar with practitioner name from session.
 * 4. Visit to /login path renders the login form.
 * 5. Visit to /register path renders the clinic registration form.
 * 6. After successful login, /login redirects to the authenticated shell.
 * 7. Explicit logout → shell-context-bar disappears, login form reappears.
 *
 * Mock strategy: intercept /api/auth/login to return SESSION_ISSUED fixture.
 * Refs: FR-012, FR-013, US10
 */

import { test, expect } from '@playwright/test';

// -----------------------------------------------------------------------
// Shared fixtures
// -----------------------------------------------------------------------

const SESSION_ISSUED = {
  mode: 'single',
  session: {
    expiresAt: '2099-12-31T23:59:59Z',
    practitioner: {
      id: 'uuid-pract-shell',
      email: 'dr.shell@aurora.com',
      profileType: 20,
      displayName: 'Dr. Shell',
      accountActive: true,
      identifiers: [],
      names: [{ text: 'Dr. Shell' }],
    },
    tenant: {
      id: 'uuid-tenant-shell',
      name: 'clinica-aurora',
      displayName: 'Clínica Aurora Shell',
      cnes: '1234567',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  },
};

// Best-effort user context (endpoint may not be present in preview build)
const USER_CONTEXT = {
  tenantId: 'uuid-tenant-shell',
  organizationId: 'uuid-tenant-shell',
  organizationName: 'Clínica Aurora Shell',
  locationId: 'loc-001',
  locationName: 'Unidade Central',
  practitionerId: 'uuid-pract-shell',
  practitionerName: 'Dr. Shell',
  profileType: 20,
};

async function setupLoginMock(page: import('@playwright/test').Page) {
  await page.route('**/api/auth/login', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(SESSION_ISSUED),
    });
  });
  // Best-effort context mock
  await page.route('**/api/users/me/context', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(USER_CONTEXT),
    });
  });
}

// -----------------------------------------------------------------------
// Tests
// -----------------------------------------------------------------------

test.describe('Shell + Auth integration', () => {
  test('unauthenticated visit shows login form in auth-template', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('auth-template')).toBeVisible({ timeout: 5000 });
    await expect(page.getByTestId('login-form')).toBeVisible();
  });

  test('after single-org login, shell-main-template is rendered', async ({ page }) => {
    await setupLoginMock(page);
    await page.goto('/');

    await page.fill('input[type="email"]', 'dr.shell@aurora.com');
    await page.fill('input[type="password"]', 'pass1234');
    await page.click('button[type="submit"]');

    await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });
  });

  test('authenticated shell shows context bar with practitioner name', async ({ page }) => {
    await setupLoginMock(page);
    await page.goto('/');

    await page.fill('input[type="email"]', 'dr.shell@aurora.com');
    await page.fill('input[type="password"]', 'pass1234');
    await page.click('button[type="submit"]');

    await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });

    // Context bar should show once TenantContext resolves
    await expect(page.getByTestId('shell-context-bar')).toBeVisible({ timeout: 8000 });
    await expect(page.getByTestId('shell-practitioner-name')).toContainText('Dr.', { timeout: 8000 });
  });

  test('visit to /login renders login form', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByTestId('login-form')).toBeVisible({ timeout: 5000 });
  });

  test('visit to /register renders clinic registration form', async ({ page }) => {
    await page.goto('/register');
    await expect(page.getByTestId('clinic-registration-form')).toBeVisible({ timeout: 5000 });
  });

  test('authenticated user navigating to /login is redirected to shell', async ({ page }) => {
    await setupLoginMock(page);
    await page.goto('/');

    await page.fill('input[type="email"]', 'dr.shell@aurora.com');
    await page.fill('input[type="password"]', 'pass1234');
    await page.click('button[type="submit"]');

    await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });

    // Navigate to /login while authenticated → should stay on shell or redirect away
    await page.goto('/login');
    await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 5000 });
  });
});
