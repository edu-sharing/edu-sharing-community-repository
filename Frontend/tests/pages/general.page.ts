import { expect, Locator, Page } from '@playwright/test';
import { testFilesFolder } from '../util/constants';
import { testStep } from '../util/test-step';
import { InlineFile } from '../util/util';
import { LoginPage } from './login.page';

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
    async sleep(ms: number) {
        await sleep(ms);
    }

    @testStep()
    async logout() {
        await this.page.locator('[data-test="main-nav-user-menu-button"]').click();
        await Promise.all([
            this.page.locator('[data-test="menu-item-LOGOUT"]').click(),
            this.page.waitForNavigation({ url: LoginPage.url }),
        ]);
    }

    @testStep()
    async expectToastMessage(message: string | RegExp) {
        await expect(this.page.locator('[data-test="toast-message"]')).toHaveText(message);
    }

    @testStep()
    async expectLoadingToFinish() {
        await this.page.waitForLoadState('networkidle');
        await expect(this.page.locator('[data-test="loading-spinner"]')).toHaveCount(0);
    }

    @testStep()
    async expectLoadingSpinner() {
        // FIXME: `toBeVisible` would be a better condition, but this fails currently, maybe because
        // to loading spinner is outside the screen.
        await expect(this.page.locator('[data-test="loading-spinner"]')).not.toHaveCount(0);
    }

    @testStep()
    async searchInTopBar(searchString: string) {
        await this.page.locator('[data-test="top-bar-search-field"]').type(searchString);
        await Promise.all([
            this.page.waitForNavigation(),
            this.page.locator('[data-test="top-bar-search-field"]').press('Enter'),
        ]);
    }

    @testStep()
    async uploadFile(fileOrFilename: string | InlineFile) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.ADD_OBJECT"]').click();
        const [fileChooser] = await Promise.all([
            this.page.waitForEvent('filechooser'),
            this.page.locator('[data-test="browse-files-button"]').click(),
        ]);
        if (typeof fileOrFilename === 'string') {
            await fileChooser.setFiles(testFilesFolder + fileOrFilename);
        } else {
            await fileChooser.setFiles(fileOrFilename);
        }
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    @testStep()
    async scrollToLastCard() {
        await this.getCardElement('').last().scrollIntoViewIfNeeded();
    }

    getCardElement(pattern: string | RegExp): Locator {
        return this.page.locator('[role="listitem"]', { hasText: pattern });
    }
}

function sleep(ms: number) {
    return new Promise<void>((resolve) => setTimeout(() => resolve(), ms));
}
