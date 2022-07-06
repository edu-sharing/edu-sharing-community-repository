import { expect, Locator, Page } from '@playwright/test';
import { testStep } from '../util/test-step';
import { GeneralPage } from './general.page';

export class WorkspacePage {
    static readonly url = './components/workspace';

    private readonly generalPage = new GeneralPage(this.page);

    constructor(private readonly page: Page) {}

    @testStep()
    async goto() {
        await Promise.all([this.page.goto(WorkspacePage.url), this.page.waitForNavigation()]);
    }

    @testStep()
    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Workspace/);
    }

    @testStep()
    async expectElement(name: string, count = 1) {
        const row = this.getElementRow(name);
        await expect(row).toHaveCount(count);
    }

    @testStep()
    async expectElementToBeSelected(pattern: string | RegExp) {
        const row = this.getElementRow(pattern);
        await expect(row.locator('input[type="checkbox"]')).toBeChecked();
    }

    @testStep()
    async expectElementNotToBeSelected(pattern: string | RegExp) {
        const row = this.getElementRow(pattern);
        await expect(row.locator('input[type="checkbox"]')).not.toBeChecked();
    }

    @testStep()
    async expectSidebarToShow(pattern: string | RegExp) {
        const sidebar = this.page.locator('[data-test="workspace-sidebar"]');
        const sidebarElementName = sidebar.locator('[data-test="workspace-sidebar-element-name"]');
        await expect(sidebar).toBeVisible();
        await expect(sidebarElementName).toHaveText(pattern);
    }

    @testStep()
    async createFolder(name: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-WORKSPACE.ADD_FOLDER"]').click();
        await this.page.locator('[data-test="add-folder-name-input"]').fill(name);
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    @testStep()
    async selectElement(pattern: string | RegExp) {
        const row = this.getElementRow(pattern);
        await row.click();
    }

    @testStep()
    async openElement(pattern: string | RegExp) {
        await Promise.all([this.getElementRow(pattern).dblclick(), this.page.waitForNavigation()]);
    }

    @testStep()
    async openElementViaMenu(pattern: string | RegExp) {
        await this.getElementRow(pattern).click({ button: 'right' });
        await Promise.all([
            this.page.locator('[data-test="menu-item-OPTIONS.SHOW"]').click(),
            this.page.waitForNavigation(),
        ]);
    }

    @testStep()
    async deleteElement(name: string) {
        const row = this.getElementRow(name);
        await row.click({ button: 'right' });
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }

    @testStep()
    async deleteSelectedElement() {
        // When we delete an element while there are still requests in flight that fetch data on
        // that element, these requests might return an error.
        // await this.page.waitForLoadState('networkidle');

        await this.page.locator('[data-test="more-actions-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
    }

    @testStep()
    async createLinkElement(url: string) {
        await this.page.locator('[data-test="top-bar-add-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.ADD_OBJECT"]').click();
        await this.page.locator('[data-test="url-input"]').type(url);
        await this.page.locator('[data-test="dialog-button-OK"]').click();
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
    }

    @testStep()
    async toggleSidebar() {
        await this.page.locator('[data-test="toggle-OPTIONS.METADATA_SIDEBAR"]').click();
    }

    private getElementRow(pattern: string | RegExp): Locator {
        return this.page.locator('[role="main"] >> [role="row"]', { hasText: pattern });
    }
}
