/**
 * T043 [P] [US3] E2e test: public clinic registration with success and CNES conflict.
 *
 * Verifies:
 * 1. Successful public registration shows confirmation message.
 * 2. Duplicate CNES submission shows CNES conflict feedback (OperationOutcome Toast/Alert).
 *
 * TDD state: RED until ClinicRegistrationForm (T046) and
 * PublicClinicRegistrationController (T044) are implemented.
 *
 * Refs: FR-003, FR-009, FR-013
 */

import { test, expect } from '@playwright/test';

test.describe('Public clinic registration', () => {
  test('successful registration shows confirmation', async ({ page }) => {
    await page.route('**/api/public/clinic-registration', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          tenantId: 'uuid-new-tenant',
          adminPractitionerId: 'uuid-new-admin',
          organization: {
            displayName: 'Clinica E2E Test',
            cnes: '9876543',
            accountActive: true,
          },
          adminPractitioner: {
            id: 'uuid-new-admin',
            email: 'admin@e2e.local',
            profileType: 10,
            displayName: 'Admin E2E',
            accountActive: true,
          },
        }),
      });
    });

    await page.goto('/registro-clinica');

    await page.fill('[aria-label*="ome da clínica" i], [placeholder*="nome" i], input[name="displayName"]', 'Clinica E2E Test');
    await page.fill('[aria-label*="CNES" i], input[name="cnes"]', '9876543');
    await page.fill('[type="email"]', 'admin@e2e.local');
    await page.fill('[aria-label*="CPF" i], input[name="cpf"]', '12345678901');
    await page.fill('[type="password"]', 'S3nha!Forte');

    await page.click('button[type="submit"]');

    // Confirmation message should appear
    await expect(page.getByRole('alert').or(page.getByText(/cadastrado/i).or(page.getByText(/sucesso/i)))).toBeVisible({ timeout: 5000 });
  });

  test('duplicate CNES shows conflict error feedback', async ({ page }) => {
    await page.route('**/api/public/clinic-registration', (route) => {
      route.fulfill({
        status: 409,
        contentType: 'application/fhir+json',
        body: JSON.stringify({
          resourceType: 'OperationOutcome',
          issue: [
            {
              severity: 'error',
              code: 'conflict',
              diagnostics: 'Tenant already exists: cnes=1234567',
            },
          ],
        }),
      });
    });

    await page.goto('/registro-clinica');

    await page.fill('[aria-label*="ome da clínica" i], [placeholder*="nome" i], input[name="displayName"]', 'Clinica Duplicada');
    await page.fill('[aria-label*="CNES" i], input[name="cnes"]', '1234567');
    await page.fill('[type="email"]', 'novo@dup.local');
    await page.fill('[aria-label*="CPF" i], input[name="cpf"]', '98765432100');
    await page.fill('[type="password"]', 'S3nha!Forte');

    await page.click('button[type="submit"]');

    // Error feedback (alert) must be visible and mention cnes or conflict
    const alert = page.getByRole('alert');
    await expect(alert).toBeVisible({ timeout: 5000 });
    const alertText = await alert.textContent();
    expect(alertText?.toLowerCase()).toMatch(/cnes|cadastrado|conflito|conflict/);
  });
});
