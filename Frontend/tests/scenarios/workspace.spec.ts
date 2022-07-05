import { test } from '@playwright/test';
import { GeneralPage } from '../pages/general.page';
import { RenderPage } from '../pages/render.page';
import { WorkspacePage } from '../pages/workspace.page';
import { defaultLogin } from '../util/constants';
import { generateTestFile, generateTestThingName, getStorageStatePath } from '../util/util';

test.use({ storageState: getStorageStatePath(defaultLogin) });

let generalPage: GeneralPage;
let workspacePage: WorkspacePage;
let renderPage: RenderPage;

test.beforeEach(async ({ page }) => {
    generalPage = new GeneralPage(page);
    workspacePage = new WorkspacePage(page);
    renderPage = new RenderPage(page);
});

test('should show workspace scope', async () => {
    await workspacePage.goto();
    await workspacePage.expectScopeButton();
});

test('should not have any warnings or errors', async () => {
    await Promise.all([generalPage.checkConsoleMessages(), workspacePage.goto()]);
});

test('should create and delete a folder', async () => {
    const folderName = generateTestThingName('folder');

    await workspacePage.goto();
    await workspacePage.createFolder(folderName);
    await workspacePage.expectElement(folderName);
    await workspacePage.deleteElement(folderName);
    await workspacePage.expectElement(folderName, 0);
});

test.describe('Empty folder', () => {
    test.beforeEach(async () => {
        const folderName = generateTestThingName('folder');

        await workspacePage.goto();
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
    });

    test('should upload a file', async () => {
        const testFile = generateTestFile();

        await generalPage.uploadFile(testFile);
        await workspacePage.expectElement(testFile.name);
    });

    test('should create a link element', async () => {
        await workspacePage.createLinkElement('http://example.org');
        await workspacePage.expectElement('Example Domain');
    });
});

test.describe('Folder with 1 element', () => {
    const testFile = generateTestFile();

    test.beforeEach(async () => {
        const folderName = generateTestThingName('folder');

        await workspacePage.goto();
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
        await generalPage.uploadFile(testFile);
    });

    test('should be selected', async () => {
        await workspacePage.expectElementToBeSelected(testFile.name);
    });

    test('should open element', async () => {
        await workspacePage.openElement(testFile.name);
        await renderPage.expectToBeOnPage();
    });

    test('should open element via menu', async () => {
        await workspacePage.openElementViaMenu(testFile.name);
        await renderPage.expectToBeOnPage();
    });

    // FIXME: Fails in Edu-Sharing
    test.skip('should show element in folder', async () => {
        await workspacePage.openElement(testFile.name);
        await renderPage.goToElementInWorkspace();
        await workspacePage.expectElementToBeSelected(testFile.name);
        await workspacePage.expectSidebarToShow(testFile.name);
    });

    // TODO: does not work for admin?
    test('should delete element', async () => {
        await workspacePage.deleteElement(testFile.name);
        await workspacePage.expectElement(testFile.name, 0);
    });

    test('should show in sidebar', async () => {
        await workspacePage.toggleSidebar();
        await workspacePage.expectSidebarToShow(testFile.name);
    });
});

test.describe('Folder with 2 elements', () => {
    const testFile1 = generateTestFile();
    const testFile2 = generateTestFile();

    test.beforeEach(async () => {
        const folderName = generateTestThingName('folder');

        await workspacePage.goto();
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
        await generalPage.uploadFile(testFile1);
        await generalPage.uploadFile(testFile2);
    });

    test('should select second element', async () => {
        await workspacePage.expectElementNotToBeSelected(testFile1.name);
        await workspacePage.expectElementToBeSelected(testFile2.name);
    });

    // FIXME: Fails in Edu-Sharing
    test.skip('should update sidebar when selecting element', async () => {
        await workspacePage.toggleSidebar();
        await workspacePage.expectSidebarToShow(testFile2.name);
        await workspacePage.selectElement(testFile1.name);
        await workspacePage.expectSidebarToShow(testFile1.name);
    });
});
