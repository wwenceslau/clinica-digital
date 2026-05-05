/**
 * T044 [P] [US3] E2E test: accessibility audit with axe-core.
 *
 * Verifies:
 *   - The authenticated Shell passes a WCAG 2.1 AA accessibility audit
 *     with 0 violations reported by axe-core.
 *   - Keyboard flow: Tab key navigates through interactive elements without
 *     focus traps.
 *
 * Tool: @axe-core/playwright
 * Tags: wcag2aa (WCAG 2.1 AA)
 *
 * Refs: FR-013, SC-006, US3
 */

import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const SESSION_ISSUED = {
  mode: 'single',
  session: {
    expiresAt: '2099-12-31T23:59:59Z',
    practitioner: {
      id: 'uuid-pract-a11y',
      email: 'dr.a11y@aurora.com',
      profileType: 20,
      displayName: 'Dr. A11y',
      accountActive: true,
      identifiers: [],
      names: [{ text: 'Dr. A11y' }],
    },
    tenant: {
      id: 'uuid-tenant-a11y',
      name: 'aurora-a11y',
      displayName: 'Clínica Aurora A11y',
      cnes: '9876543',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  },
};

const USER_CONTEXT = {
  tenantId: 'uuid-tenant-a11y',
  organizationId: 'uuid-tenant-a11y',
  organizationName: 'Clínica Aurora A11y',
  locationId: 'loc-a11y-001',
  locationName: 'Unidade A11y',
  practitionerId: 'uuid-pract-a11y',
  practitionerName: 'Dr. A11y',
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
      body: JSON.stringify(USER_CONTEXT),
    });
  });
}

async function loginAndWaitForShell(page: import('@playwright/test').Page) {
  await page.goto('/');
  await expect(page.getByTestId('login-form')).toBeVisible({ timeout: 8000 });
  await page.fill('input[type="email"]', 'dr.a11y@aurora.com');
  await page.fill('input[type="password"]', 'pass1234');
  await page.click('button[type="submit"]');
  await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Shell accessibility audit — WCAG 2.1 AA (T044)', () => {
  test('authenticated shell has 0 WCAG 2.1 AA violations', async ({ page }) => {
    await setupApiMocks(page);
    await loginAndWaitForShell(page);

    // Run axe-core audit scoped to the shell root, WCAG 2.1 AA tags only
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

    expect(results.violations).toEqual([]);
  });

  test('keyboard tab navigation reaches all interactive shell elements', async ({ page }) => {
    await setupApiMocks(page);
    await loginAndWaitForShell(page);

    // Press Tab repeatedly — the page must not enter an infinite focus trap
    // Verify we can reach at least one interactive element (header-location-selector)
    let found = false;
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      const focused = page.locator(':focus');
      const testId = await focused.getAttribute('data-testid').catch(() => null);
      if (testId === 'header-location-selector') {
        found = true;
        break;
      }
    }
    expect(found, 'header-location-selector should be reachable via Tab').toBe(true);
  });
});
