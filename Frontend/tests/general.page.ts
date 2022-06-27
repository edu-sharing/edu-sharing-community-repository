import { expect, Page } from '@playwright/test';

export class GeneralPage {
    constructor(private readonly page: Page) {}

    /**
     * Checks if any warnings or errors are printed to the console for `seconds`.
     *
     * Defaults to 12 seconds to wait for a warning about constant change detection, which is
     * printed after 10 seconds.
     */
    async checkConsoleMessages(seconds: number = 12) {
        await Promise.race([timeout(seconds * 1000), this.checkConsoleMessagesIndefinitely()]);
    }

    async expectToastMessage(message: string | RegExp) {
        await expect(this.page.locator('[data-test="toast-message"]')).toHaveText(message);
    }

    private async checkConsoleMessagesIndefinitely(): Promise<void> {
        const msg = await this.page.waitForEvent('console', { timeout: 0 });
        expect(msg.type()).not.toBe('warning');
        expect(msg.type()).not.toBe('error');
        await this.checkConsoleMessagesIndefinitely();
    }
}

function timeout(ms: number): Promise<void> {
    return new Promise((resolve) => {
        setTimeout(() => resolve(), ms);
    });
}
