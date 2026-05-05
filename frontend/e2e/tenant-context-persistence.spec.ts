/**
 * T072 [US5] E2E test: tenant context persists in shell header after login.
 *
 * Verifies that after a successful login:
 * 1. The shell context bar (data-testid="shell-context-bar") is visible.
 * 2. The organisation name (data-testid="shell-org-name") is displayed.
 * 3. The practitioner name (data-testid="shell-practitioner-name") is displayed.
 *
 * Uses a mocked login API response so no backend is required.
 *
 * TDD state: RED until MainTemplate renders the context bar (T077).
 *
 * Refs: FR-007, US5; specs/004-institution-iam-auth-integration/tasks.md T072
 */

import { test, expect } from '@playwright/test';

const MOCK_SESSION = {
  sessionId: '550e8400-e29b-41d4-a716-446655440000',
  expiresAt: new Date(Date.now() + 3600_000).toISOString(),
  mode: 'single',
  practitioner: {
    id: 'prac-uuid-001',
    email: 'medico@t072.local',
    profileType: 20,
    displayName: 'Dr. T072 Practitioner',
    accountActive: true,
    identifiers: [],
    names: [{ use: 'official', text: 'Dr. T072 Practitioner' }],
  },
  tenant: {
    id: '660e8400-e29b-41d4-a716-446655440001',
    name: 't072-clinic',
    displayName: 'Clínica T072',
    cnes: 'T072001',
    active: true,
    accountActive: true,
    identifiers: [],
  },
};

test.describe('Tenant context persistence in shell header (T072)', () => {
  test.beforeEach(async ({ page }) => {
    // Mock the login endpoint so tests don't require a running backend
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_SESSION),
      });
    });
  });

  test('shell-context-bar appears with org name after login', async ({ page }) => {
    await page.goto('/');

    // Fill and submit the login form
    await page.fill('[name="email"], [type="email"]', 'medico@t072.local');
    await page.fill('[type="password"]', 'qualquer');
    await page.click('button[type="submit"]');

    // The shell context bar must be visible
    await expect(page.getByTestId('shell-context-bar')).toBeVisible({ timeout: 5000 });

    // Organisation name must appear in the header
    const orgNameEl = page.getByTestId('shell-org-name');
    await expect(orgNameEl).toBeVisible({ timeout: 5000 });
    await expect(orgNameEl).toContainText('Clínica T072');
  });

  test('shell-context-bar shows practitioner name after login', async ({ page }) => {
    await page.goto('/');

    await page.fill('[name="email"], [type="email"]', 'medico@t072.local');
    await page.fill('[type="password"]', 'qualquer');
    await page.click('button[type="submit"]');

    await expect(page.getByTestId('shell-context-bar')).toBeVisible({ timeout: 5000 });

    const practitionerNameEl = page.getByTestId('shell-practitioner-name');
    await expect(practitionerNameEl).toBeVisible({ timeout: 5000 });
    await expect(practitionerNameEl).toContainText('Dr. T072 Practitioner');
  });

  test('shell-context-bar is absent before login', async ({ page }) => {
    await page.goto('/');

    // The shell context bar must NOT appear on the login screen
    const contextBar = page.getByTestId('shell-context-bar');
    await expect(contextBar).not.toBeVisible();
  });
});
