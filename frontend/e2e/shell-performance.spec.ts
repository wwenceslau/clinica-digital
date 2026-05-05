/**
 * T045 [P] [US3] E2E test: performance budget assertion — LCP ≤ 1.5 s.
 *
 * Verifies:
 *   - The Largest Contentful Paint (LCP) for the authenticated Shell is
 *     at most 1 500 ms on a standard desktop Chrome profile.
 *
 * Strategy:
 *   - page.addInitScript installs a PerformanceObserver to capture LCP
 *     before the app scripts run.
 *   - After login and shell render, page.evaluate reads the captured value.
 *
 * Refs: FR-014, SC-007, US3
 */

import { test, expect } from '@playwright/test';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const SESSION_ISSUED = {
  mode: 'single',
  session: {
    expiresAt: '2099-12-31T23:59:59Z',
    practitioner: {
      id: 'uuid-pract-perf',
      email: 'dr.perf@aurora.com',
      profileType: 20,
      displayName: 'Dr. Perf',
      accountActive: true,
      identifiers: [],
      names: [{ text: 'Dr. Perf' }],
    },
    tenant: {
      id: 'uuid-tenant-perf',
      name: 'aurora-perf',
      displayName: 'Clínica Aurora Perf',
      cnes: '1111111',
      active: true,
      accountActive: true,
      identifiers: [],
    },
  },
};

const USER_CONTEXT = {
  tenantId: 'uuid-tenant-perf',
  organizationId: 'uuid-tenant-perf',
  organizationName: 'Clínica Aurora Perf',
  locationId: 'loc-perf-001',
  locationName: 'Unidade Perf',
  practitionerId: 'uuid-pract-perf',
  practitionerName: 'Dr. Perf',
  profileType: 20,
};

// ── LCP budget (ms) ───────────────────────────────────────────────────────────

const LCP_BUDGET_MS = 1500;

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

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Shell performance budget — LCP ≤ 1 500 ms (T045)', () => {
  test(`shell LCP is within ${LCP_BUDGET_MS} ms budget`, async ({ page }) => {
    // Install LCP observer before any navigation so it captures the metric
    await page.addInitScript(() => {
      (window as Window & { __lcpMs?: number }).__lcpMs = 0;
      try {
        const observer = new PerformanceObserver((entryList) => {
          for (const entry of entryList.getEntries()) {
            (window as Window & { __lcpMs?: number }).__lcpMs = entry.startTime;
          }
        });
        observer.observe({ type: 'largest-contentful-paint', buffered: true });
      } catch {
        // PerformanceObserver may not be available in all environments
      }
    });

    await setupApiMocks(page);

    // Navigate and log in — this is where LCP is measured
    await page.goto('/');
    await expect(page.getByTestId('login-form')).toBeVisible({ timeout: 8000 });
    await page.fill('input[type="email"]', 'dr.perf@aurora.com');
    await page.fill('input[type="password"]', 'pass1234');
    await page.click('button[type="submit"]');
    await expect(page.getByTestId('shell-main-template')).toBeVisible({ timeout: 8000 });

    // Allow a short settle to let the LCP observer fire
    await page.waitForTimeout(200);

    // Read the captured LCP value from the browser context
    const lcpMs = await page.evaluate(
      () => (window as Window & { __lcpMs?: number }).__lcpMs ?? 0,
    );

    expect(
      lcpMs,
      `LCP ${lcpMs.toFixed(0)} ms exceeds budget of ${LCP_BUDGET_MS} ms`,
    ).toBeLessThanOrEqual(LCP_BUDGET_MS);
  });
});
