import { expect, Locator, Page } from '@playwright/test';
import { testStep } from '../util/test-step';

export class GeneralPage {
    constructor(private readonly page: Page) {}

    /**
     * Checks if any warnings or errors are printed to the console for `seconds`.
     *
     * Defaults to 12 seconds to wait for a warning about constant change detection, which is
     * printed after 10 seconds.
     */
    @testStep({ failOnConsoleError: true, failOnConsoleWarning: true })
    async checkConsoleMessages(seconds: number = 12) {
        await sleep(seconds * 1000);
    }

    @testStep()
    async sleep(seconds: number) {
        await sleep(seconds * 1000);
    }

    @testStep()
    async expectToastMessage(message: string | RegExp) {
        await expect(this.page.locator('[data-test="toast-message"]')).toHaveText(message);
    }

    @testStep()
    async searchInTopBar(searchString: string) {
        await this.page.locator('[data-test="top-bar-search-field"]').type(searchString);
        await Promise.all([
            this.page.waitForNavigation(),
            this.page.locator('[data-test="top-bar-search-field"]').press('Enter'),
        ]);
    }

    getCardElement(pattern: string | RegExp): Locator {
        return this.page.locator('[role="listitem"]', { hasText: pattern });
    }
}

function sleep(ms: number) {
    return new Promise<void>((resolve) => setTimeout(() => resolve(), ms));
}
