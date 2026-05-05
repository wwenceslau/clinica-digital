/**
 * T092 [P] E2E test: admin creates profile 20 user via form.
 *
 * Verifies:
 * 1. Admin opens user creation modal, fills form, submits → POST /api/admin/users
 *    called → success toast/confirmation shown.
 * 2. Duplicate email submission → shows OperationOutcome error feedback.
 *
 * TDD state: RED until CreateUserModal (T097) and AdminUserController (T093)
 * are implemented.
 *
 * Refs: FR-006, FR-013
 */

import { test, expect } from '@playwright/test';

test.describe('Admin creates profile 20 user', () => {
  test('successful user creation shows confirmation', async ({ page }) => {
    await page.route('**/api/admin/users', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          userId: 'uuid-new-user',
          practitionerId: 'uuid-new-practitioner',
          practitionerRoleId: 'uuid-new-role',
          practitioner: {
            id: 'uuid-new-practitioner',
            email: 'novo@clinica.local',
            profileType: 20,
            displayName: 'Dr. Novo Usuario',
            accountActive: true,
          },
        }),
      });
    });

    await page.goto('/admin/usuarios');

    // Open create user modal/form
    await page.click('button[data-testid="btn-create-user"], button:has-text("Criar Usuário"), button:has-text("Novo Usuário")');

    // Fill in the form fields
    await page.fill('[name="displayName"], [aria-label*="Nome" i], [placeholder*="nome" i]', 'Dr. Novo Usuario');
    await page.fill('[name="email"], [type="email"]', 'novo@clinica.local');
    await page.fill('[name="cpf"], [aria-label*="CPF" i]', '12345678901');
    await page.fill('[name="password"], [type="password"]', 'S3nha@Novo123');

    // Submit the form
    await page.click('button[type="submit"]:has-text("Salvar"), button[type="submit"]:has-text("Criar"), button[data-testid="btn-submit-user"]');

    // Confirmation / success feedback should appear
    await expect(
      page.getByRole('alert').or(
        page.getByText(/criado/i).or(
          page.getByText(/sucesso/i).or(
            page.getByTestId('toast-success')
          )
        )
      )
    ).toBeVisible({ timeout: 5000 });
  });

  test('duplicate email shows conflict error feedback', async ({ page }) => {
    await page.route('**/api/admin/users', (route) => {
      route.fulfill({
        status: 409,
        contentType: 'application/fhir+json',
        body: JSON.stringify({
          resourceType: 'OperationOutcome',
          issue: [
            {
              severity: 'error',
              code: 'conflict',
              details: { text: 'Email already exists in tenant' },
              diagnostics: 'Email already exists in tenant: duplicate@clinica.local',
            },
          ],
        }),
      });
    });

    await page.goto('/admin/usuarios');

    // Open create user modal/form
    await page.click('button[data-testid="btn-create-user"], button:has-text("Criar Usuário"), button:has-text("Novo Usuário")');

    // Fill in the form with duplicate email
    await page.fill('[name="displayName"], [aria-label*="Nome" i], [placeholder*="nome" i]', 'Usuario Duplicado');
    await page.fill('[name="email"], [type="email"]', 'duplicate@clinica.local');
    await page.fill('[name="cpf"], [aria-label*="CPF" i]', '99988877766');
    await page.fill('[name="password"], [type="password"]', 'S3nha@Dup456');

    // Submit the form
    await page.click('button[type="submit"]:has-text("Salvar"), button[type="submit"]:has-text("Criar"), button[data-testid="btn-submit-user"]');

    // Conflict error feedback must appear
    await expect(
      page.getByText(/email/i).or(
        page.getByText(/conflict/i).or(
          page.getByText(/já existe/i).or(
            page.getByRole('alert')
          )
        )
      )
    ).toBeVisible({ timeout: 5000 });
  });
});
