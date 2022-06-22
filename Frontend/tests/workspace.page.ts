import { expect, Page } from '@playwright/test';

export class WorkspacePage {
    static readonly url = './components/workspace';

    constructor(private readonly page: Page) {}

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Workspace/);
    }

    async createFolder(name: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-WORKSPACE.ADD_FOLDER"]').click();
        await this.page.locator('[data-test="add-folder-name-input"]').fill(name);
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    async expectElement(name: string, count = 1) {
        const cell = this.page.locator('[data-test="table-cell-cm:name"]', { hasText: name });
        await expect(cell).toHaveCount(count);
    }

    async deleteElement(name: string) {
        const cell = this.page.locator('[data-test="table-cell-cm:name"]', { hasText: name });
        await cell.click({ button: 'right' });
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }

    async deleteSelectedElement() {
        await this.page.locator('[data-test="more-actions-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }
}
