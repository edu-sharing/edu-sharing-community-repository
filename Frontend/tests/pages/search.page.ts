import { expect, Locator, Page } from '@playwright/test';
import { testStep } from '../util/test-step';
import { GeneralPage } from './general.page';

export class SearchPage {
    static readonly url = './components/search';
    /** The time after which we expect the search index to reflect any changes. */
    static readonly INDEX_UPDATE_TIMEOUT = 10 * 1000;

    private readonly generalPage = new GeneralPage(this.page);

    constructor(private readonly page: Page) {}

    @testStep()
    async goto({ expectMaterials = true } = {}) {
        await this.page.goto(SearchPage.url);
        if (expectMaterials) {
            // Wait for materials to be visible be default, so we can check effectively for items to
            // _not_ be shown.
            await expect(this.page.locator('h2 >> text=/Materials/')).toBeVisible();
        }
    }

    @testStep()
    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Search/);
    }

    @testStep()
    async expectToHaveElement(pattern: string | RegExp) {
        await expect(this.getElement(pattern)).toBeVisible();
    }

    @testStep()
    async expectNotToHaveElement(pattern: string | RegExp) {
        await expect(this.getElement(pattern)).not.toBeVisible();
    }

    @testStep()
    async expectNoSearchResults() {
        await expect(this.page.locator('text=No results for this search query.')).toBeVisible();
    }

    @testStep()
    async expectToFindElementBySearching(pattern: string, count = 1) {
        const foundElements = await this.searchForElement(pattern);
        expect(foundElements).toBe(count);
    }

    // Not a test step since exclusively composed of other test steps.
    async expectToEventuallyHaveElement(name: string) {
        await expect
            .poll(
                async () => {
                    await this.goto();
                    return this.generalPage.getCardElement(name).count();
                },
                {
                    message: `expect to eventually have ${name}`,
                    timeout: SearchPage.INDEX_UPDATE_TIMEOUT,
                },
            )
            .toBe(1);
    }

    // Not a test step since exclusively composed of other test steps.
    async expectToEventuallyFindBySearching(pattern: string) {
        await expect
            .poll(
                async () => {
                    await this.goto();
                    return this.searchForElement(pattern);
                },
                {
                    message: `expect to eventually find ${pattern} by searching`,
                    timeout: SearchPage.INDEX_UPDATE_TIMEOUT,
                },
            )
            .toBe(1);
    }

    private getElement(pattern: string | RegExp): Locator {
        return this.generalPage.getCardElement(pattern);
    }

    /**
     * @returns number of matches visible on the results page
     */
    private async searchForElement(pattern: string): Promise<number> {
        await this.generalPage.searchInTopBar(pattern);
        // Explicitly wait for the page to stop loading, so we can use the resulting number in
        // `expect.poll` statements.
        await this.generalPage.expectLoadingToFinish();
        return this.generalPage.getCardElement(pattern).count();
    }
}
