/**
 * T052 [P] [US4] E2e test: IAM login multi-tenant with org selection.
 *
 * Verifies:
 * 1. Invalid credentials → 401 OperationOutcome error displayed.
 * 2. Single-org login → session issued, main view rendered.
 * 3. Multi-org login → inline org-selection form appears.
 * 4. Select organization → session established, main view rendered.
 * 5. Rate-limit lockout (429) → throttle error displayed.
 * 6. No-org user (401) → error message displayed.
 *
 * Uses Page Object Models: LoginPage, OrganizationSelectPage.
 * Refs: FR-004, FR-013
 */

import { test, expect } from '@playwright/test';
import { LoginPage } from '../src/test/iam/LoginPage';
import { OrganizationSelectPage } from '../src/test/iam/OrganizationSelectPage';

// ---------------------------------------------------------------------------
// Shared mock data
// ---------------------------------------------------------------------------

const SESSION_ISSUED = {
  expiresAt: '2099-12-31T23:59:59Z',
  practitioner: {
    id: 'uuid-pract-1',
    email: 'user@clinica.local',
    profileType: 20,
    displayName: 'Dr. Teste',
    accountActive: true,
    identifiers: [],
    names: [{ text: 'Dr. Teste' }],
  },
  tenant: {
    id: 'uuid-tenant-1',
    name: 'clinica-aurora',
    displayName: 'Clínica Aurora',
    cnes: '1234567',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

const MULTI_ORG_RESPONSE = {
  mode: 'multiple',
  challengeToken: 'challenge-token-abc123',
  organizations: [
    { organizationId: 'uuid-org-1', displayName: 'Clínica Aurora', cnes: '1234567' },
    { organizationId: 'uuid-org-2', displayName: 'Clínica Horizonte', cnes: '7654321' },
  ],
};

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe('IAM login – multi-tenant flow', () => {
  test('invalid credentials show 401 OperationOutcome error', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/fhir+json',
        body: JSON.stringify({
          resourceType: 'OperationOutcome',
          issue: [{ severity: 'error', code: 'security', diagnostics: 'Credenciais inválidas.' }],
        }),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('wrong@clinica.local');
    await loginPage.fillPassword('wrong-password');
    await loginPage.submit();

    await expect(page.locator('[role="alert"]')).toBeVisible({ timeout: 5000 });
    const errorMsg = await loginPage.getErrorMessage();
    expect(errorMsg).toContain('Credenciais inválidas');
  });

  test('single-org login renders main view', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ mode: 'single', session: SESSION_ISSUED }),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('admin@aurora.local');
    await loginPage.fillPassword('S3nha!Forte');
    await loginPage.submit();

    await expect(
      page.locator('[data-testid="shell-main-template"], [data-testid="tenant-admin"]'),
    ).toBeVisible({ timeout: 5000 });
  });

  test('multi-org login shows inline organization selection', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MULTI_ORG_RESPONSE),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('multi@clinica.local');
    await loginPage.fillPassword('S3nha!Forte');
    await loginPage.submit();

    const orgSelectPage = new OrganizationSelectPage(page);
    const loaded = await orgSelectPage.assertPageLoaded();
    expect(loaded).toBe(true);

    const orgs = await orgSelectPage.getAvailableOrganizations();
    expect(orgs.length).toBeGreaterThanOrEqual(2);
  });

  test('select organization establishes session and renders main view', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MULTI_ORG_RESPONSE),
      });
    });

    await page.route('**/api/auth/select-organization', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(SESSION_ISSUED),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('multi@clinica.local');
    await loginPage.fillPassword('S3nha!Forte');
    await loginPage.submit();

    const orgSelectPage = new OrganizationSelectPage(page);
    await orgSelectPage.assertPageLoaded();
    await orgSelectPage.selectOrganization('Clínica Aurora');
    await orgSelectPage.submit();

    await expect(
      page.locator('[data-testid="shell-main-template"], [data-testid="tenant-admin"]'),
    ).toBeVisible({ timeout: 5000 });
  });

  test('rate-limit lockout (429) shows throttle error', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 429,
        contentType: 'application/fhir+json',
        body: JSON.stringify({
          resourceType: 'OperationOutcome',
          issue: [
            {
              severity: 'error',
              code: 'throttled',
              diagnostics: 'Limite de tentativas excedido. Tente novamente em 15 minutos.',
            },
          ],
        }),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('locked@clinica.local');
    await loginPage.fillPassword('wrong-password');
    await loginPage.submit();

    await expect(page.locator('[role="alert"]')).toBeVisible({ timeout: 5000 });
    const errorMsg = await loginPage.getErrorMessage();
    expect(errorMsg).toContain('Limite de tentativas');
  });

  test('user without active organization shows 401 error', async ({ page }) => {
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/fhir+json',
        body: JSON.stringify({
          resourceType: 'OperationOutcome',
          issue: [
            {
              severity: 'error',
              code: 'security',
              diagnostics: 'Usuário sem organização ativa associada.',
            },
          ],
        }),
      });
    });

    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('norg@clinica.local');
    await loginPage.fillPassword('S3nha!Forte');
    await loginPage.submit();

    await expect(page.locator('[role="alert"]')).toBeVisible({ timeout: 5000 });
    const errorMsg = await loginPage.getErrorMessage();
    expect(errorMsg).toContain('sem organização ativa');
  });
});
