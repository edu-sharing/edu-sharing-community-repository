import { expect, Page } from '@playwright/test';

export class WorkspacePage {
    static readonly url = './components/workspace';

    constructor(private readonly page: Page) {}

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Workspace/);
    }
}
