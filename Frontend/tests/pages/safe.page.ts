import { expect, Page } from '@playwright/test';
import { testStep } from '../util/test-step';
import { LoginCredentials } from '../util/util';
import { GeneralPage } from './general.page';
import { LoginPage } from './login.page';

export class SafePage {
    static readonly url = './components/workspace/safe';
    private readonly generalPage = new GeneralPage(this.page);
    private readonly loginPage = new LoginPage(this.page);

    constructor(private readonly page: Page) {}

    @testStep()
    async goto(loginCredentials: LoginCredentials) {
        await Promise.all([
            this.page.goto(SafePage.url),
            this.page.waitForNavigation({ url: /\/components\/login\?scope=safe/ }),
        ]);
        await Promise.all([
            this.loginPage.loginPasswordOnly(loginCredentials),
            this.page.waitForNavigation({ url: /\/components\/workspace\/safe/ }),
        ]);
    }

    @testStep()
    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Safe/);
    }
}
