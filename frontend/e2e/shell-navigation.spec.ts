import { expect, test } from "@playwright/test";

test("sidebar navigation flow expands domain and changes route", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Acessar" }).click();

  const securityDomainToggle = page.getByRole("button", { name: "Seguranca" });
  await securityDomainToggle.click();

  const resourceItem = page.getByRole("menuitem", { name: "Gestao de Usuarios Internos" });
  await resourceItem.click();

  await expect(page).toHaveURL(/\/admin\/security\/users$/);
});
