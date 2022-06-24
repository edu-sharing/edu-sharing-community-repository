import { expect, Locator, Page } from '@playwright/test';
import { testStep } from '../util/test-step';
import { InlineFile } from '../util/util';
import { GeneralPage } from './general.page';

export class CollectionsPage {
    static readonly url = './components/collections';

    private readonly generalPage = new GeneralPage(this.page);

    constructor(private readonly page: Page) {}

    @testStep()
    async expectScopeButton() {
        const mainNavScopeButton = this.page.locator('[data-test="main-nav-scope-button"]');
        await expect(mainNavScopeButton).toHaveText(/Collections/);
    }

    @testStep()
    async expectToBeOnRootCollectionPage() {
        await expect(this.page).toHaveURL(/\/collections/);
        await expect(this.page).not.toHaveURL(/id=\w/);
    }

    @testStep()
    async expectToBeOnCollectionPage(name?: string) {
        await expect(this.page).toHaveURL(/\/collections/);
        if (name) {
            await expect(this.page.locator('h1')).toHaveText(name);
        }
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
    async addPrivateCollection(name: string) {
        await this.page.locator('[data-test="card-button-OPTIONS\\.NEW_COLLECTION"]').click();
        await this.page.locator('[data-test="create-private-collection-button"]').click();
        await this.page.locator('[data-test="new-collection-name-input"]').fill(name);
        await Promise.all([
            this.page.waitForNavigation(),
            this.page.locator('[data-test="dialog-button-SAVE"]').click(),
        ]);
    }

    @testStep()
    async deleteCurrentCollection() {
        await this.page.locator('[data-test="more-actions-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.DELETE"]').click();
        await this.page.locator('[data-test="dialog-button-YES_DELETE"]').click();
        await this.generalPage.expectToastMessage('Element(s) moved to recycle');
    }

    @testStep()
    async uploadFileToCurrentCollection(
        file: InlineFile,
        { editMetadata = false, delayEditMetadataSeconds = 0 } = {},
    ) {
        await this.page.locator('[data-test="card-button-OPTIONS.ADD_OBJECT"]').click();
        const [fileChooser] = await Promise.all([
            this.page.waitForEvent('filechooser'),
            this.page.locator('[data-test="browse-files-button"]').click(),
        ]);
        await fileChooser.setFiles(file);
        if (editMetadata) {
            await this.page.locator('[data-test="more-metadata-button"]').click();
            await this.generalPage.sleep(delayEditMetadataSeconds);
        }
        await this.page.locator('[data-test="dialog-button-SAVE"]').click();
        await this.generalPage.expectToastMessage(
            /1 element\(s\) have been added to the collection/,
        );
    }

    @testStep()
    async addElementToCurrentCollection(name: string, { searchForElement = true } = {}) {
        await this.page.locator('[data-test="card-button-OPTIONS.SEARCH_OBJECT"]').click();
        if (searchForElement) {
            await this.generalPage.searchInTopBar(name);
        }
        await Promise.all([
            this.getElement(name)
                .locator('[data-test="option-button-SEARCH.ADD_INTO_COLLECTION_SHORT"]')
                .first()
                .click(),
            this.page.waitForNavigation({ url: /\/collections/ }),
        ]);
    }

    @testStep()
    async removeElementFromCurrentCollection(pattern: string | RegExp) {
        await this.getElement(pattern).locator('[data-test="card-options-button"]').click();
        await this.page.locator('[data-test="menu-item-OPTIONS.REMOVE_REF"]').click();
        await this.generalPage.expectToastMessage(
            'The element(s) have been removed from the collection',
        );
    }

    @testStep()
    async goToElementInWorkspace(pattern: string | RegExp) {
        await this.getElement(pattern).locator('[data-test="card-options-button"]').click();
        await Promise.all([
            this.page.locator('[data-test="menu-item-OPTIONS.SHOW_IN_FOLDER"]').click(),
            this.page.waitForNavigation({ url: /\/workspace/ }),
        ]);
    }

    private getElement(pattern: string | RegExp): Locator {
        return this.generalPage.getCardElement(pattern);
    }
}
