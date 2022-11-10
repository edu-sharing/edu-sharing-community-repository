import { expect, test } from '@playwright/test';
import { CollectionsPage } from '../pages/collections.page';
import { GeneralPage } from '../pages/general.page';
import { SearchPage } from '../pages/search.page';
import { WorkspacePage } from '../pages/workspace.page';
import { defaultLogin } from '../util/constants';
import {
    generateTestFile,
    generateTestThingName,
    getBaseName,
    getStorageStatePath,
} from '../util/util';

test.use({ storageState: getStorageStatePath(defaultLogin) });

let generalPage: GeneralPage;
let searchPage: SearchPage;
let collectionsPage: CollectionsPage;
let workspacePage: WorkspacePage;

test.beforeEach(async ({ page }) => {
    generalPage = new GeneralPage(page);
    searchPage = new SearchPage(page);
    collectionsPage = new CollectionsPage(page);
    workspacePage = new WorkspacePage(page);
});

test('should go to search page', async () => {
    await searchPage.goto({ expectMaterials: true });
});

test('should show search scope', async () => {
    await searchPage.goto();
    await generalPage.expectScopeButton(/Search/);
});

// FIXME: Fails due to console error.
test.skip('should show an uploaded file', async () => {
    const testFile = generateTestFile();

    await searchPage.goto();
    await generalPage.uploadFile(testFile);
    await searchPage.expectToHaveElement(testFile.name);
});

// FIXME: Flaky due to failing 'should show an uploaded file'.
test.skip('should show an uploaded file after reload', async () => {
    const testFile = generateTestFile();

    await searchPage.goto();
    await generalPage.uploadFile(testFile);
    await searchPage.expectToEventuallyHaveElement(testFile.name);
});

// FIXME: Flaky due to failing 'should show an uploaded file'.
test.skip('should find an uploaded file', async () => {
    const testFile = generateTestFile();

    await searchPage.goto();
    await generalPage.uploadFile(testFile);
    await searchPage.expectToEventuallyFindBySearching(getBaseName(testFile.name));
});

// Depends on https://issues.edu-sharing.net/jira/browse/DESP-819
test.skip('should find an uploaded file by full filename', async () => {
    const testFile = generateTestFile();

    await searchPage.goto();
    await generalPage.uploadFile(testFile);
    await searchPage.expectToEventuallyFindBySearching(testFile.name);
});

// FIXME: flaky when run in parallel (poll timeout too small?)
test('should show a file uploaded to a collection', async () => {
    const testFile = generateTestFile();
    const collectionName = generateTestThingName('collection');

    await collectionsPage.goto();
    await collectionsPage.addPrivateCollection(collectionName);
    await collectionsPage.uploadFileToCurrentCollection(testFile);

    await searchPage.expectToEventuallyFindBySearching(getBaseName(testFile.name));
});

// FIXME: flaky
//
// 500 response
test('should not show a file uploaded to a collection and then deleted', async () => {
    const testFile = generateTestFile();
    const collectionName = generateTestThingName('collection');

    await collectionsPage.goto();
    await collectionsPage.addPrivateCollection(collectionName);
    await collectionsPage.uploadFileToCurrentCollection(testFile);

    await collectionsPage.goToElementInWorkspace(testFile.name);
    await workspacePage.deleteSelectedElement();

    // Wait for the search index to settle
    await generalPage.sleep(SearchPage.INDEX_UPDATE_TIMEOUT);
    await searchPage.goto();
    await searchPage.expectNotToHaveElement(testFile.name);
    await generalPage.searchInTopBar(getBaseName(testFile.name));
    await searchPage.expectNoSearchResults();
});

test('should show no results when searching for non-existent file', async () => {
    const testFile = generateTestFile();

    await searchPage.goto();
    await generalPage.searchInTopBar(testFile.name);
    await searchPage.expectNotToHaveElement(testFile.name);
    await searchPage.expectNoSearchResults();
});
