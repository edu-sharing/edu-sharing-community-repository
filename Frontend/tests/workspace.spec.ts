import { test } from '@playwright/test';
import { defaultLogin, testFile1 } from './constants';
import { GeneralPage } from './general.page';
import { RenderPage } from './render.page';
import { generateTestThingName, getStorageStatePath } from './util';
import { WorkspacePage } from './workspace.page';

test.use({ storageState: getStorageStatePath(defaultLogin) });

test('should show workspace scope', async ({ page }) => {
    await page.goto(WorkspacePage.url);
    const workspacePage = new WorkspacePage(page);
    await workspacePage.expectScopeButton();
});

test('should not have any warnings or errors', async ({ page }) => {
    await Promise.all([new GeneralPage(page).checkConsoleMessages(), page.goto(WorkspacePage.url)]);
});

test('should create and delete a folder', async ({ page }) => {
    await page.goto(WorkspacePage.url);
    const folderName = generateTestThingName('folder');
    const workspacePage = new WorkspacePage(page);
    await workspacePage.createFolder(folderName);
    await workspacePage.expectElement(folderName);
    await workspacePage.deleteElement(folderName);
    await workspacePage.expectElement(folderName, 0);
});

test.describe('Empty folder', () => {
    test.beforeEach(async ({ page }) => {
        const folderName = generateTestThingName('folder');
        const workspacePage = new WorkspacePage(page);

        await page.goto(WorkspacePage.url);
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
    });

    test('should upload a file', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);

        await workspacePage.uploadFile(testFile1);
        await workspacePage.expectElement(testFile1);
    });

    test('should create a link element', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);

        await workspacePage.createLinkElement('http://example.org');
        await workspacePage.expectElement('example.org');
    });
});

test.describe('Folder with 1 element', () => {
    const elementName = 'example.org';

    test.beforeEach(async ({ page }) => {
        const folderName = generateTestThingName('folder');
        const workspacePage = new WorkspacePage(page);

        await page.goto(WorkspacePage.url);
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
        await workspacePage.createLinkElement('http://example.org');
    });

    test('should open element', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);
        const renderPage = new RenderPage(page);

        await workspacePage.openElement(elementName);
        await renderPage.expectToBeOnPage();
    });

    test('should open element via menu', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);
        const renderPage = new RenderPage(page);

        await workspacePage.openElementViaMenu(elementName);
        await renderPage.expectToBeOnPage();
    });

    test('should show element in folder', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);
        const renderPage = new RenderPage(page);

        await workspacePage.openElement(elementName);
        await renderPage.goToElementInWorkspace();
        await workspacePage.expectElementToBeSelected(elementName);
        await workspacePage.expectSidebarToShow(elementName);
    });

    test('should delete element', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);

        await workspacePage.deleteElement(elementName);
        await workspacePage.expectElement(elementName, 0);
    });

    test('should show in sidebar', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);

        await workspacePage.selectElement(elementName);
        await workspacePage.toggleSidebar();
        await workspacePage.expectSidebarToShow(elementName);
    });
});

test.describe('Folder with 2 elements', () => {
    const elementName1 = 'example.org';
    const elementName2 = 'example.com';

    test.beforeEach(async ({ page }) => {
        const folderName = generateTestThingName('folder');
        const workspacePage = new WorkspacePage(page);

        await page.goto(WorkspacePage.url);
        await workspacePage.createFolder(folderName);
        await workspacePage.openElement(folderName);
        await workspacePage.createLinkElement('http://example.org');
        await workspacePage.createLinkElement('http://example.com');
    });

    test('should update sidebar when selecting element', async ({ page }) => {
        const workspacePage = new WorkspacePage(page);

        await workspacePage.selectElement(elementName1);
        await workspacePage.toggleSidebar();
        await workspacePage.expectSidebarToShow(elementName1);
        await workspacePage.selectElement(elementName2);
        await workspacePage.expectSidebarToShow(elementName2);
    });
});
