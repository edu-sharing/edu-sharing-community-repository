import { expect, test } from '@playwright/test';
import { CollectionsPage } from '../pages/collections.page';
import { GeneralPage } from '../pages/general.page';
import { WorkspacePage } from '../pages/workspace.page';
import { defaultLogin } from '../util/constants';
import {
    generateTestFile,
    generateTestThingName,
    getStorageStatePath,
    InlineFile,
} from '../util/util';

test.use({ storageState: getStorageStatePath(defaultLogin) });

let generalPage: GeneralPage;
let collectionsPage: CollectionsPage;
let workspacePage: WorkspacePage;

test.beforeEach(async ({ page }) => {
    generalPage = new GeneralPage(page);
    collectionsPage = new CollectionsPage(page);
    workspacePage = new WorkspacePage(page);
});

test('should not have any warnings or errors', async () => {
    await Promise.all([generalPage.checkConsoleMessages(), collectionsPage.goto()]);
});

test.describe('Root collections page', () => {
    test.beforeEach(async () => {
        await collectionsPage.goto();
    });

    test('should be root page', async () => {
        await collectionsPage.expectToBeOnRootCollectionPage();
    });

    test('should show collections scope', async () => {
        await generalPage.expectScopeButton(/Collections/);
    });

    // FIXME: This needs more than 25 root collections as setup
    test.skip('should load more collections on scroll', async () => {
        await generalPage.expectLoadingToFinish();
        // FIXME: Page interaction outside test step
        const elements = await generalPage.getCardElement('').count();
        await generalPage.scrollToLastCard();
        await generalPage.expectLoadingSpinner();
        await generalPage.expectLoadingToFinish();
        expect(await generalPage.getCardElement('').count()).toBeGreaterThan(elements);
    });

    test('should create a collection', async () => {
        const collectionName = generateTestThingName('collection');

        await collectionsPage.addPrivateCollection(collectionName);
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
    });

    test('should show a created a collection after reload', async () => {
        const collectionName = generateTestThingName('collection');

        await collectionsPage.addPrivateCollection(collectionName);
        await collectionsPage.expectToEventuallyHaveElement(collectionName);
    });
});

test.describe('Empty collection', () => {
    let collectionName: string;

    test.beforeEach(async () => {
        collectionName = generateTestThingName('collection');

        await collectionsPage.goto();
        await collectionsPage.addPrivateCollection(collectionName);
    });

    // FIXME: Fails in Edu-Sharing.
    test.skip('should delete a collection', async () => {
        await collectionsPage.deleteCurrentCollection();
        await collectionsPage.expectToBeOnRootCollectionPage();
    });

    test('should upload an element', async () => {
        const testFile = generateTestFile();

        await collectionsPage.uploadFileToCurrentCollection(testFile);
        await generalPage.expectToastMessage(
            `1 element(s) have been added to the collection "${collectionName}" ` +
                `and are visible for you`,
        );
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectToHaveElement(testFile.name);
    });

    // FIXME: Fails in Edu-Sharing.
    test.skip('should upload an element with metadata editor', async () => {
        const testFile = generateTestFile();

        await collectionsPage.uploadFileToCurrentCollection(testFile, {
            editMetadata: true,
            // There is a bug that only occurs when slightly waiting before opening the metadata
            // editor. We want to test for this bug here.
            delayEditMetadata: 2000,
        });
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectToHaveElement(testFile.name);
    });

    // FIXME: Fails, probably due to the element not being available.
    test.skip('should add an existing element', async () => {
        const elementName = 'example.org';

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

    test.beforeEach(async () => {
        testFile = generateTestFile();
        collectionName = generateTestThingName('collection');

        await collectionsPage.goto();
        await collectionsPage.addPrivateCollection(collectionName);
        await collectionsPage.uploadFileToCurrentCollection(testFile);
    });

    test('should remove an element from the collection', async () => {
        await collectionsPage.removeElementFromCurrentCollection(testFile.name);
        await collectionsPage.expectToBeOnCollectionPage(collectionName);
        await collectionsPage.expectNotToHaveElement(testFile.name);
    });

    // FIXME: Fails in Edu-Sharing.
    test.skip('should mark an element as removed from workspace', async ({ page }) => {
        const collectionPageUrl = page.url();

        await collectionsPage.goToElementInWorkspace(testFile.name);
        await workspacePage.deleteSelectedElement();
        // FIXME: page interaction outside test step
        await Promise.all([page.goBack(), page.waitForNavigation({ url: collectionPageUrl })]);
        // TODO expect "element deleted" banner
        expect(false).toBeTruthy();
    });
});
