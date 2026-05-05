/**
 * T033 [P] [US2] E2E test: location change persistence across page reload.
 *
 * Verifies:
 * (a) User selects location B (pre-seeded in localStorage to simulate selection).
 * (b) Page reloads (session cleared, user re-authenticates).
 * (c) Location B is still active in the location selector.
 *
 * Strategy:
 *   - page.addInitScript seeds localStorage before each navigation so that
 *     resolveActiveLocation() restores loc-B on every page load.
 *   - API mocks return loc-B so it is present in availableLocations.
 *
 * Refs: FR-012, SC-003, US2
 */

import { test, expect } from '@playwright/test';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const TENANT_ID  = 'uuid-tenant-t033';
const PRACT_ID   = 'uuid-pract-t033';
const LOC_B_ID   = 'loc-b-t033';
const LOC_B_NAME = 'Unidade Sul T033';

const STORAGE_KEY = `shell.active_location.${TENANT_ID}.${PRACT_ID}`;

const SESSION_ISSUED = {
  mode: 'single',
  session: {
    expiresAt: '2099-12-31T23:59:59Z',
    practitioner: {
      id: PRACT_ID,
      email: 'dr.t033@aurora.com',
      profileType: 20,
      displayName: 'Dr. T033',
      accountActive: true,
      identifiers: [],
      names: [{ text: 'Dr. T033' }],
    },
    tenant: {
      id: TENANT_ID,
      name: 'aurora-t033',
      displayName: 'Clínica Aurora T033',
      cnes: '1234567',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  },
};

const USER_CONTEXT_LOC_B = {
  tenantId: TENANT_ID,
  organizationId: TENANT_ID,
  organizationName: 'Clínica Aurora T033',
  locationId: LOC_B_ID,
  locationName: LOC_B_NAME,
  practitionerId: PRACT_ID,
  practitionerName: 'Dr. T033',
  profileType: 20,
};

// ── Helpers ───────────────────────────────────────────────────────────────────

async function setupApiMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/auth/login', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(SESSION_ISSUED),
    });
  });

  await page.route('**/api/users/me/context', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(USER_CONTEXT_LOC_B),
    });
  });
}

async function doLogin(page: import('@playwright/test').Page) {
  // Ensure login form is visible (redirected by ProtectedRoute when unauthenticated)
  await expect(page.getByTestId('login-form')).toBeVisible({ timeout: 8000 });
  await page.fill('input[type="email"]', 'dr.t033@aurora.com');
  await page.fill('input[type="password"]', 'pass1234');
  await page.click('button[type="submit"]');
  await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Location persistence across page reload (T033)', () => {
  /**
   * Pre-seeds localStorage with loc-B before every navigation.
   * The app's resolveActiveLocation() should then restore loc-B on load.
   */
  test('persists selected location across page reload', async ({ page }) => {
    // (a) Seed localStorage: simulates user having previously selected loc-B
    await page.addInitScript(
      ({ key, value }: { key: string; value: string }) => {
        window.localStorage.setItem(key, value);
      },
      { key: STORAGE_KEY, value: LOC_B_ID },
    );

    await setupApiMocks(page);

    // First load — session cleared, start from login
    await page.goto('/');
    await doLogin(page);

    // Location B should be active in the selector
    const selector = page.getByTestId('header-location-selector');
    await expect(selector).toBeVisible({ timeout: 5000 });
    await expect(selector).toContainText(LOC_B_NAME);

    // (b) Simulate reload: navigate back to root (React state / session reset)
    await page.goto('/');

    // (c) After reload + re-login, location B must still be active
    await doLogin(page);

    const selectorAfterReload = page.getByTestId('header-location-selector');
    await expect(selectorAfterReload).toBeVisible({ timeout: 5000 });
    await expect(selectorAfterReload).toContainText(LOC_B_NAME);
  });
});
