import { test } from '@playwright/test';
import { defaultLogin } from './constants';
import { GeneralPage } from './general.page';
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

// TODO: test create link
// TODO: test upload file
// TODO: show in folder
//   - is selected
// TODO: sidebar
//   - changes when selecting new element
