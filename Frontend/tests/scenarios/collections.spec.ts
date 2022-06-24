import { expect, test } from '@playwright/test';
import { CollectionsPage } from '../pages/collections.page';
import { defaultLogin } from '../util/constants';
import { GeneralPage } from '../pages/general.page';
import {
    generateTestFile,
    generateTestThingName,
    getStorageStatePath,
    InlineFile,
} from '../util/util';
import { WorkspacePage } from '../pages/workspace.page';

test.use({ storageState: getStorageStatePath(defaultLogin) });

test('should go to root collections page', async ({ page }) => {
    const collectionsPage = new CollectionsPage(page);

    await page.goto(CollectionsPage.url);
    await collectionsPage.expectToBeOnRootCollectionPage();
});

test('should show collections scope', async ({ page }) => {
    const collectionsPage = new CollectionsPage(page);

    await page.goto(CollectionsPage.url);
    await collectionsPage.expectScopeButton();
});

test('should not have any warnings or errors', async ({ page }) => {
    await Promise.all([
        new GeneralPage(page).checkConsoleMessages(),
        page.goto(CollectionsPage.url),
    ]);
});

test('should create a collection', async ({ page }) => {
    const collectionName = generateTestThingName('collection');
    const collectionsPage = new CollectionsPage(page);

    await page.goto(CollectionsPage.url);
    await collectionsPage.addPrivateCollection(collectionName);
    await collectionsPage.expectToBeOnCollectionPage(collectionName);
});

test.describe('Empty collection', () => {
    let collectionName: string;

    test.beforeEach(async ({ page }) => {
        collectionName = generateTestThingName('collection');
        const collectionsPage = new CollectionsPage(page);

        await page.goto(CollectionsPage.url);
        await collectionsPage.addPrivateCollection(collectionName);
    });

    test('should delete a collection', async ({ page }) => {
        const collectionsPage = new CollectionsPage(page);

        await collectionsPage.deleteCurrentCollection();
        await collectionsPage.expectToBeOnRootCollectionPage();
    });

    test('should upload an element', async ({ page }) => {
        const testFile = generateTestFile();
        const collectionsPage = new CollectionsPage(page);

        await collectionsPage.uploadFileToCurrentCollection(testFile);
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectToHaveElement(testFile.name);
    });

    test('should upload an element with metadata editor', async ({ page }) => {
        const testFile = generateTestFile();
        const collectionsPage = new CollectionsPage(page);

        await collectionsPage.uploadFileToCurrentCollection(testFile, {
            editMetadata: true,
            // There is a bug that only occurs when slightly waiting before opening the metadata
            // editor. We want to test for this bug here.
            delayEditMetadataSeconds: 2,
        });
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectToHaveElement(testFile.name);
    });

    test('should add an existing element', async ({ page }) => {
        const elementName = 'example.org';
        const collectionsPage = new CollectionsPage(page);

        // TODO: make sure the element is available
        await collectionsPage.addElementToCurrentCollection(elementName, {
            searchForElement: false,
        });
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectToHaveElement(elementName);
    });
});

test.describe('Collection with 1 element', () => {
    let collectionName: string;
    let testFile: InlineFile;

    test.beforeEach(async ({ page }) => {
        testFile = generateTestFile();
        collectionName = generateTestThingName('collection');
        const collectionsPage = new CollectionsPage(page);

        await page.goto(CollectionsPage.url);
        await collectionsPage.addPrivateCollection(collectionName);
        await collectionsPage.uploadFileToCurrentCollection(testFile);
    });

    test('should remove an element from the collection', async ({ page }) => {
        const collectionsPage = new CollectionsPage(page);

        await collectionsPage.removeElementFromCurrentCollection(testFile.name);
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectNotToHaveElement(testFile.name);
    });

    test('should mark an element as removed from workspace', async ({ page }) => {
        const collectionsPage = new CollectionsPage(page);
        const workspacePage = new WorkspacePage(page);
        const collectionPageUrl = page.url();

        await collectionsPage.goToElementInWorkspace(testFile.name);
        await workspacePage.deleteSelectedElement();
        await Promise.all([page.goBack(), page.waitForNavigation({ url: collectionPageUrl })]);
        // TODO expect "element deleted" banner
        expect(false).toBeTruthy();
    });
});
