/**
 * T084 [US11] E2E test: active location selection in shell header.
 *
 * Verifies:
 * 1. After login, when GET /api/users/me/context returns a locationName,
 *    the header shows it via [data-testid="shell-location-name"].
 * 2. Selecting a different location via POST /api/users/me/active-location
 *    updates the [data-testid="shell-location-name"] in the header.
 *
 * TDD state: RED until:
 *   - TenantContext.tsx fetches /api/users/me/context on mount (T088)
 *   - MainTemplate.tsx renders shell-location-name (T088)
 *
 * Refs: FR-008, FR-018, FR-019, US11
 */

import { test, expect } from '@playwright/test';

const TENANT_ID = 'a0000000-0000-4000-8000-000000000001';
const SESSION_ID = 'b0000000-0000-4000-8000-000000000002';

const CONTEXT_RESPONSE = {
  tenantId: TENANT_ID,
  organizationId: TENANT_ID,
  organizationName: 'Hospital T084',
  locationId: 'c0000000-0000-4000-8000-000000000003',
  locationName: 'Pronto-Socorro T084',
  practitionerId: 'd0000000-0000-4000-8000-000000000004',
  practitionerName: 'Dr. T084 Test',
  profileType: 20,
  practitionerRoleId: 'e0000000-0000-4000-8000-000000000005',
  roleCode: 'MD',
};

const UPDATED_CONTEXT_RESPONSE = {
  ...CONTEXT_RESPONSE,
  locationId: 'f0000000-0000-4000-8000-000000000006',
  locationName: 'UTI T084',
  practitionerRoleId: 'e0000000-0000-4000-8000-000000000007',
};

test.describe('Active location selection in shell header', () => {
  /**
   * Scenario 1: when the context API returns a locationName,
   * the shell header displays it in [data-testid="shell-location-name"].
   */
  test('displays locationName in shell header after login', async ({ page }) => {
    // Mock authentication API
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: { 'Set-Cookie': `cd_session=${SESSION_ID}; HttpOnly; SameSite=Lax; Path=/` },
        body: JSON.stringify({
          sessionId: SESSION_ID,
          practitioners: [
            {
              id: CONTEXT_RESPONSE.practitionerId,
              displayName: CONTEXT_RESPONSE.practitionerName,
              organizations: [
                { id: TENANT_ID, displayName: CONTEXT_RESPONSE.organizationName, tenantId: TENANT_ID },
              ],
            },
          ],
        }),
      });
    });

    // Mock select-organization (single-org flow returns session right away)
    await page.route('**/api/auth/select-organization', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sessionId: SESSION_ID,
          tenantId: TENANT_ID,
          organizationId: TENANT_ID,
          organizationName: CONTEXT_RESPONSE.organizationName,
          practitionerName: CONTEXT_RESPONSE.practitionerName,
          profileType: 20,
        }),
      });
    });

    // Mock GET /api/users/me/context — returns locationName
    await page.route('**/api/users/me/context', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(CONTEXT_RESPONSE),
      });
    });

    // Navigate to protected area (redirect to login if unauthenticated)
    await page.goto('/');

    // Login flow
    await page.fill('[type="email"]', 'dr@t084.local');
    await page.fill('[type="password"]', 'S3nha@T084');
    await page.click('button[type="submit"]');

    // Shell context bar must show locationName
    await expect(page.getByTestId('shell-location-name'))
      .toBeVisible({ timeout: 8000 });
    await expect(page.getByTestId('shell-location-name'))
      .toContainText('Pronto-Socorro T084');
  });

  /**
   * Scenario 2: after calling setActiveLocation, the shell header updates
   * [data-testid="shell-location-name"] to the new location's name.
   */
  test('updates shell-location-name after selecting new location', async ({ page }) => {
    const newLocationId = UPDATED_CONTEXT_RESPONSE.locationId;

    // Mock GET /api/users/me/context — initially returns first location
    await page.route('**/api/users/me/context', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(CONTEXT_RESPONSE),
      });
    });

    // Mock POST /api/users/me/active-location — returns updated context
    await page.route('**/api/users/me/active-location', (route) => {
      const request = route.request();
      if (request.method() === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(UPDATED_CONTEXT_RESPONSE),
        });
      } else {
        route.continue();
      }
    });

    // Mock login
    await page.route('**/api/auth/login', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: { 'Set-Cookie': `cd_session=${SESSION_ID}; HttpOnly; SameSite=Lax; Path=/` },
        body: JSON.stringify({
          sessionId: SESSION_ID,
          practitioners: [
            {
              id: CONTEXT_RESPONSE.practitionerId,
              displayName: CONTEXT_RESPONSE.practitionerName,
              organizations: [
                { id: TENANT_ID, displayName: CONTEXT_RESPONSE.organizationName, tenantId: TENANT_ID },
              ],
            },
          ],
        }),
      });
    });

    await page.route('**/api/auth/select-organization', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sessionId: SESSION_ID,
          tenantId: TENANT_ID,
          organizationId: TENANT_ID,
          organizationName: CONTEXT_RESPONSE.organizationName,
          practitionerName: CONTEXT_RESPONSE.practitionerName,
          profileType: 20,
        }),
      });
    });

    await page.goto('/');

    // Login flow
    await page.fill('[type="email"]', 'dr@t084.local');
    await page.fill('[type="password"]', 'S3nha@T084');
    await page.click('button[type="submit"]');

    // Verify initial location name is shown
    await expect(page.getByTestId('shell-location-name'))
      .toContainText('Pronto-Socorro T084', { timeout: 8000 });

    // Trigger location change via exposed test hook or UI element
    // The TenantContext exposes setActiveLocation; the shell may expose a location picker
    // Here we evaluate the function directly via page.evaluate to simulate a programmatic call
    await page.evaluate(
      ([locId]) => {
        // Dispatch a custom event that the app listens to (if implemented as a hook)
        window.dispatchEvent(new CustomEvent('cd:setActiveLocation', { detail: { locationId: locId } }));
      },
      [newLocationId]
    );

    // After the POST completes, locationName should update
    await expect(page.getByTestId('shell-location-name'))
      .toContainText('UTI T084', { timeout: 8000 });
  });
});
