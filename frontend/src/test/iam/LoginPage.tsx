/**
 * LoginPage Test Fixture - Feature 004 IAM Integration
 * 
 * Provides page object model (POM) for Login flows in e2e tests.
 * Supports contract testing and scenario automation.
 */

export class LoginPage {
  constructor(private page: any) {}

  /**
   * Navigate to login page
   */
  async goto() {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Fill email field
   */
  async fillEmail(email: string) {
    await this.page.fill('input[type="email"]', email);
  }

  /**
   * Fill password field
   */
  async fillPassword(password: string) {
    await this.page.fill('input[type="password"]', password);
  }

  /**
   * Submit login form
   */
  async submit() {
    await this.page.click('button[type="submit"]');
    // Wait for response or navigation
    await this.page.waitForNavigation({ waitUntil: 'networkidle' }).catch(() => {});
  }

  /**
   * Get error message (FHIR OperationOutcome as Toast/Alert)
   */
  async getErrorMessage(): Promise<string> {
    const errorElement = await this.page.$('div[role="alert"]');
    return errorElement ? await errorElement.textContent() : '';
  }

  /**
   * Assert login success (navigated to organization selection or dashboard)
   */
  async assertLoginSuccess() {
    const url = this.page.url();
    return url.includes('/organization-select') || url.includes('/dashboard');
  }

  /**
   * Assert login error feedback
   */
  async assertLoginError(expectedErrorCode?: string) {
    const errorMsg = await this.getErrorMessage();
    return errorMsg.length > 0 && (!expectedErrorCode || errorMsg.includes(expectedErrorCode));
  }
}

/**
 * Fixture Factory: Create and initialize LoginPage
 */
export async function createLoginPageFixture(page: any): Promise<LoginPage> {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  return loginPage;
}
