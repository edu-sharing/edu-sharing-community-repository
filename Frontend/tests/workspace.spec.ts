import { test } from '@playwright/test';
import { GeneralPage } from './general.page';
import { WorkspacePage } from './workspace.page';

test.use({ storageState: 'playwright/storage/admin.json' });

test('should show workspace scope', async ({ page }) => {
    await page.goto(WorkspacePage.url);
    const workspacePage = new WorkspacePage(page);
    await workspacePage.expectScopeButton();
});

test('should not have any warnings or errors', async ({ page }) => {
    await Promise.all([new GeneralPage(page).checkConsoleMessages(), page.goto(WorkspacePage.url)]);
});
