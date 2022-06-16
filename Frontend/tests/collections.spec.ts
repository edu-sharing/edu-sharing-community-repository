import { test } from '@playwright/test';
import { CollectionsPage } from './collections.page';
import { defaultLogin } from './constants';
import { GeneralPage } from './general.page';
import { getStorageStatePath } from './util';

test.use({ storageState: getStorageStatePath(defaultLogin) });

test('should show collections scope', async ({ page }) => {
    await page.goto(CollectionsPage.url);
    const collectionsPage = new CollectionsPage(page);
    await collectionsPage.expectScopeButton();
});

test('should not have any warnings or errors', async ({ page }) => {
    await Promise.all([
        new GeneralPage(page).checkConsoleMessages(),
        page.goto(CollectionsPage.url),
    ]);
});
