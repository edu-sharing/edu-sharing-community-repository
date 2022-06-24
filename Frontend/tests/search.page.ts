import { expect, Locator, Page } from '@playwright/test';
import { GeneralPage } from './general.page';
import { InlineFile, sleep } from './util';

export class SearchPage {
    static readonly url = './components/search';

    private readonly generalPage = new GeneralPage(this.page);

    constructor(private readonly page: Page) {}

    async goto({ expectMaterials = true } = {}) {
        await this.page.goto(SearchPage.url);
        if (expectMaterials) {
            // Wait for materials to be visible be default, so we can check effectively for items to
            // _not_ be shown.
            await expect(this.page.locator('h2 >> text=/Materials/')).toBeVisible();
        }
    }

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Search/);
    }

    async expectToHaveElement(pattern: string | RegExp) {
        await expect(this.getElement(pattern)).toBeVisible();
    }

    async expectNotToHaveElement(pattern: string | RegExp) {
        await expect(this.getElement(pattern)).not.toBeVisible();
    }

    async expectNoSearchResults() {
        await expect(this.page.locator('text=No results for this search query.')).toBeVisible();
    }

    private getElement(pattern: string | RegExp): Locator {
        return this.generalPage.getCardElement(pattern);
    }
}
