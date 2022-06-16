import { expect, Page } from '@playwright/test';

export class CollectionsPage {
    static readonly url = './components/collections';

    constructor(private readonly page: Page) {}

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Collections/);
    }

    async addPrivateCollection(name: string) {
        await this.page.locator('[data-test="card-button-OPTIONS\\.NEW_COLLECTION"]').click();
        await this.page.locator('[data-test="create-private-collection-button"]').click();
        await this.page.locator('[data-test="new-collection-name-input"]').fill(name);
        await Promise.all([
            this.page.waitForNavigation(),
            this.page.locator('[data-test="dialog-button-SAVE"]').click(),
        ]);
    }

    async expectToBeOnCollectionPage(name: string) {
        await expect(this.page).toHaveURL(/\/collections/);
        await expect(this.page.locator('h1')).toHaveText(name);
    }
}
