/**
 * OrganizationSelectPage Test Fixture - Feature 004 IAM Integration
 * 
 * Provides page object model (POM) for Organization Selection flows.
 * Supports contract testing for multi-tenant context resolution.
 */

export class OrganizationSelectPage {
  constructor(private page: any) {}

  /**
   * Assert page loaded (challenge token in session or URL param)
   */
  async assertPageLoaded() {
    await this.page.waitForSelector('[data-testid="organization-select-form"]');
    return true;
  }

  /**
   * Get list of available organizations
   */
  async getAvailableOrganizations(): Promise<string[]> {
    const options = await this.page.$$('select option, [role="option"]');
    const orgNames: string[] = [];
    for (const option of options) {
      const text = await option.textContent();
      if (text && !text.includes('Select')) {
        orgNames.push(text.trim());
      }
    }
    return orgNames;
  }

  /**
   * Select organization by name
   */
  async selectOrganization(organizationName: string) {
    const selectElement = await this.page.$('select');
    if (selectElement) {
      await selectElement.selectOption(organizationName);
    } else {
      // Radio button or custom component
      await this.page.click(`[data-testid="org-select-${organizationName}"]`);
    }
  }

  /**
   * Submit organization selection
   */
  async submit() {
    await this.page.click('button[type="submit"]');
    await this.page.waitForNavigation({ waitUntil: 'networkidle' }).catch(() => {});
  }

  /**
   * Assert success (session cookie set, navigated to dashboard)
   */
  async assertSelectionSuccess() {
    const url = this.page.url();
    const cookies = await this.page.context().cookies();
    const sessionCookie = cookies.find((c: any) => c.name.includes('session'));
    return url.includes('/dashboard') && !!sessionCookie;
  }

  /**
   * Get error message
   */
  async getErrorMessage(): Promise<string> {
    const errorElement = await this.page.$('div[role="alert"]');
    return errorElement ? await errorElement.textContent() : '';
  }
}

/**
 * Fixture Factory
 */
export async function createOrganizationSelectPageFixture(page: any): Promise<OrganizationSelectPage> {
  const orgSelectPage = new OrganizationSelectPage(page);
  await orgSelectPage.assertPageLoaded();
  return orgSelectPage;
}
