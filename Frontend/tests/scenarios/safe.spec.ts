import { test } from '@playwright/test';
import { GeneralPage } from '../pages/general.page';
import { LoginPage } from '../pages/login.page';
import { SafePage } from '../pages/safe.page';
import { defaultLogin } from '../util/constants';
import { EduSharingApi } from '../util/edu-sharing-api';

let generalPage: GeneralPage;
let loginPage: LoginPage;
let safePage: SafePage;

// test.beforeAll(async ({ browser }) => {});

test.beforeEach(async ({ browser, page }) => {
    generalPage = new GeneralPage(page);
    loginPage = new LoginPage(page);
    safePage = new SafePage(page);

    // FIXME: This might fail when running tests in parallel and our tool permissions are reset at
    // the wrong time. If we can fix that, we can run this in the `beforeAll` hook.
    const api = new EduSharingApi(browser);
    await api.resetToolPermissions({ TOOLPERMISSION_CONFIDENTAL: 'ALLOWED' });
    // We cannot use `storageState` because the backend will assign new session IDs when we enter or
    // leave the save, so we log in the old fashioned way each time.
    await page.goto(LoginPage.url);
    await loginPage.login(defaultLogin);
});

// FIXME: Fails in edu-sharing.
test.skip('should not have any warnings or errors', async () => {
    await Promise.all([generalPage.checkConsoleMessages(), safePage.goto(defaultLogin)]);
});
