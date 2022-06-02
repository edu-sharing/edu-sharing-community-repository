import { test } from '@playwright/test';
import { WorkspacePage } from './workspace.page';

test.use({ storageState: 'playwright/storage/admin.json' });

test('should show workspace scope', async ({ page }) => {
    await page.goto(WorkspacePage.url);
    const workspacePage = new WorkspacePage(page);
    await workspacePage.expectScopeButton();
});
