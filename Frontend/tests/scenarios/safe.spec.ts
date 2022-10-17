import { test } from '@playwright/test';
import { GeneralPage } from '../pages/general.page';
import { LoginPage } from '../pages/login.page';
import { SafePage } from '../pages/safe.page';
import { defaultLogin } from '../util/constants';
import { EduSharingApi } from '../util/edu-sharing-api';

let generalPage: GeneralPage;
let loginPage: LoginPage;
let safePage: SafePage;

test.beforeAll(async ({ browser }) => {
    // FIXME: This fails when running tests in this file in parallel because the `beforeAll` hook
    // will be executed simultaneously by multiple workers, which causes a database error in
    // edu-sharing. Additionally, this will fail when tool permissions are reset at the wrong time
    // by other test files.
    const api = new EduSharingApi(browser);
    await api.resetToolPermissions({ TOOLPERMISSION_CONFIDENTAL: 'ALLOWED' });
});

// TODO: Remove (see above).
test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({ page }) => {
    generalPage = new GeneralPage(page);
    loginPage = new LoginPage(page);
    safePage = new SafePage(page);

    // We cannot use `storageState` because the backend will assign new session IDs when we enter or
    // leave the save, so we log in the old fashioned way each time.
    await page.goto(LoginPage.url);
    await loginPage.login(defaultLogin);
});

test('should login to safe', async () => {
    await safePage.goto(defaultLogin);
    // We don't check for warnings since the timer will always trigger the change-detection warning.
});

test('should show safe scope', async () => {
    await safePage.goto(defaultLogin);
    await generalPage.expectScopeButton(/Safe/);
});

test('should show logout timer', async () => {
    // Note that the logout time is hidden via CSS an small screen sizes, but the element is still
    // present within the DOM.
    await safePage.goto(defaultLogin);
    await safePage.expectLogoutTimer('9:59');
    await safePage.expectLogoutTimer('9:58');
});
