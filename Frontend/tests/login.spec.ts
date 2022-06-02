import { test, expect } from '@playwright/test';
import { LoginPage } from './login.page';
import { WorkspacePage } from './workspace.page';

test('should redirect to login page', async ({ page }) => {
    await Promise.all([page.goto('.'), page.waitForNavigation({ url: LoginPage.url })]);
});

test('should go to workspace after login', async ({ page }) => {
    await page.goto(LoginPage.url);
    await Promise.all([
        new LoginPage(page).login('admin', 'admin'),
        page.waitForNavigation({ url: WorkspacePage.url }),
    ]);
});

test('should show workspace scope after login', async ({ page }) => {
    await page.goto(LoginPage.url);
    await new LoginPage(page).login('admin', 'admin');
    await new WorkspacePage(page).expectScopeButton();
});
