import { test } from '@playwright/test';
import { CollectionsPage } from '../pages/collections.page';
import { GeneralPage } from '../pages/general.page';
import { SearchPage } from '../pages/search.page';
import { WorkspacePage } from '../pages/workspace.page';
import { defaultLogin } from '../util/constants';
import { generateTestFile, generateTestThingName, getStorageStatePath } from '../util/util';

test.use({ storageState: getStorageStatePath(defaultLogin) });

test('should go to search page', async ({ page }) => {
    const searchPage = new SearchPage(page);

    await searchPage.goto({ expectMaterials: true });
});

test('should show search scope', async ({ page }) => {
    const searchPage = new SearchPage(page);

    await searchPage.goto();
    await searchPage.expectScopeButton();
});

test('should show a file uploaded to a collection', async ({ page }) => {
    // esTest(page, async () => {
    const testFile = generateTestFile();
    const collectionName = generateTestThingName('collection');
    const collectionsPage = new CollectionsPage(page);
    const searchPage = new SearchPage(page);
    const generalPage = new GeneralPage(page);

    await page.goto(CollectionsPage.url);
    await collectionsPage.addPrivateCollection(collectionName);
    await collectionsPage.uploadFileToCurrentCollection(testFile);

    // Wait for the search index to settle
    await generalPage.sleep(10);
    await searchPage.goto();
    // await searchPage.expectToHaveElement(testFile.name);
    await generalPage.searchInTopBar(testFile.name);
    await searchPage.expectToHaveElement(testFile.name);
    // }));
});

test('should not show a file uploaded to a collection and then deleted', async ({ page }) => {
    const testFile = generateTestFile();
    const collectionName = generateTestThingName('collection');
    const collectionsPage = new CollectionsPage(page);
    const searchPage = new SearchPage(page);
    const generalPage = new GeneralPage(page);
    const workspacePage = new WorkspacePage(page);

    await page.goto(CollectionsPage.url);
    await collectionsPage.addPrivateCollection(collectionName);
    await collectionsPage.uploadFileToCurrentCollection(testFile);

    await collectionsPage.goToElementInWorkspace(testFile.name);
    await workspacePage.deleteSelectedElement();

    // Wait for the search index to settle
    await generalPage.sleep(10);
    await searchPage.goto();
    await searchPage.expectNotToHaveElement(testFile.name);
    await generalPage.searchInTopBar(testFile.name);
    await searchPage.expectNoSearchResults();
});

test('should show no results when searching for non-existent file', async ({ page }) => {
    const testFile = generateTestFile();
    const searchPage = new SearchPage(page);
    const generalPage = new GeneralPage(page);

    await searchPage.goto();
    await generalPage.searchInTopBar(testFile.name);
    await searchPage.expectNotToHaveElement(testFile.name);
    await searchPage.expectNoSearchResults();
});
