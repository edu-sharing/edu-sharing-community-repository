import { expect, Page } from '@playwright/test';

export class CollectionsPage {
    static readonly url = './components/collections';

    constructor(private readonly page: Page) {}

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Collections/);
    }
}
