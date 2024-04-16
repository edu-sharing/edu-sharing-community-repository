import { expect, Page } from '@playwright/test';
import { testStep } from '../util/test-step';

export class RenderPage {
    static readonly url = './components/render';

    constructor(private readonly page: Page) {}

    @testStep()
    async expectToBeOnPage() {
        await expect(this.page).toHaveURL(new RegExp(RenderPage.url));
    }

    @testStep()
    async goToElementInWorkspace() {
        await this.page.locator('[data-test="more-actions-button"]').click();
        await Promise.all([
            this.page.locator('[data-test="menu-item-OPTIONS.SHOW_IN_FOLDER"]').click(),
            this.page.waitForNavigation({ url: /\/workspace/ }),
        ]);
    }
}
