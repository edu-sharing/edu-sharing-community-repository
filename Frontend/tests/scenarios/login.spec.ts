import { test } from '@playwright/test';
import { defaultLogin } from '../util/constants';
import { GeneralPage } from '../pages/general.page';
import { LoginPage } from '../pages/login.page';
import { WorkspacePage } from '../pages/workspace.page';

test('should redirect to login page', async ({ page }) => {
    await Promise.all([page.goto('.'), page.waitForNavigation({ url: LoginPage.url })]);
});

test('should not have any warnings or errors', async ({ page }) => {
    await Promise.all([new GeneralPage(page).checkConsoleMessages(), page.goto(LoginPage.url)]);
});

test('should go to workspace after login', async ({ page }) => {
    await page.goto(LoginPage.url);
    await Promise.all([
        new LoginPage(page).login(defaultLogin),
        page.waitForNavigation({ url: WorkspacePage.url }),
    ]);
});

test('should show workspace scope after login', async ({ page }) => {
    await page.goto(LoginPage.url);
    await new LoginPage(page).login(defaultLogin);
    await new WorkspacePage(page).expectScopeButton();
});
