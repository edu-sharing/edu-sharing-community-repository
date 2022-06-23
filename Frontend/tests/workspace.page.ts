import { expect, Locator, Page } from '@playwright/test';
import { testFilesFolder } from './constants';
import { GeneralPage } from './general.page';

export class WorkspacePage {
    static readonly url = './components/workspace';

    private readonly generalPage = new GeneralPage(this.page);

    constructor(private readonly page: Page) {}

    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Workspace/);
    }

    async expectElement(name: string, count = 1) {
        const row = this.getElementRow(name);
        await expect(row).toHaveCount(count);
    }

    async expectElementToBeSelected(pattern: string | RegExp) {
        const row = this.getElementRow(pattern);
        await expect(row.locator('input[type="checkbox"]')).toBeChecked();
    }

    async expectSidebarToShow(pattern: string | RegExp) {
        const sidebar = this.page.locator('[data-test="workspace-sidebar"]');
        const sidebarElementName = sidebar.locator('[data-test="workspace-sidebar-element-name"]');
        await expect(sidebar).toBeVisible();
        await expect(sidebarElementName).toHaveText(pattern);
    }

    async createFolder(name: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-WORKSPACE.ADD_FOLDER"]').click();
        await this.page.locator('[data-test="add-folder-name-input"]').fill(name);
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    async selectElement(pattern: string | RegExp) {
        const row = this.getElementRow(pattern);
        await row.click();
    }

    async openElement(pattern: string | RegExp) {
        await Promise.all([this.getElementRow(pattern).dblclick(), this.page.waitForNavigation()]);
    }

    async openElementViaMenu(pattern: string | RegExp) {
        await this.getElementRow(pattern).click({ button: 'right' });
        await Promise.all([
            this.page.locator('[data-test="menu-item-OPTIONS.SHOW"]').click(),
            this.page.waitForNavigation(),
        ]);
    }

    async deleteElement(name: string) {
        const row = this.getElementRow(name);
        await row.click({ button: 'right' });
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }

    async deleteSelectedElement() {
        await this.page.locator('[data-test="more-actions-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }

    async uploadFile(fileName: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.ADD_OBJECT"]').click();
        const [fileChooser] = await Promise.all([
            this.page.waitForEvent('filechooser'),
            this.page.locator('[data-test="browse-files-button"]').click(),
        ]);
        await fileChooser.setFiles(testFilesFolder + fileName);
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    async createLinkElement(url: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.ADD_OBJECT"]').click();
        await this.page.locator('[data-test="url-input"]').type(url);
        await this.page.locator('[data-test="dialog-button-OK"]').click();
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    async toggleSidebar() {
        await this.page.locator('[data-test="toggle-OPTIONS.METADATA_SIDEBAR"]').click();
    }

    private getElementRow(pattern: string | RegExp): Locator {
        return this.page.locator('[role="main"] >> [role="row"]', { hasText: pattern });
    }
}
