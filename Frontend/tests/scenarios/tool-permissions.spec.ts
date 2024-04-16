import { test } from '@playwright/test';
import { GeneralPage } from '../pages/general.page';
import { defaultLogin } from '../util/constants';
import { EduSharingApi } from '../util/edu-sharing-api';
import { saveLogin } from '../util/saveLogin';
import { getStorageStatePath } from '../util/util';

let generalPage: GeneralPage;

test.describe.skip('Tool permission "foo"', () => {
    const storageStatePath = getStorageStatePath(defaultLogin, '-foo');

    test.beforeAll(async ({ browser, baseURL }) => {
        const api = new EduSharingApi(browser);
        // await api.resetToolPermissions({ TOOLPERMISSION_FOO: 'ALLOWED' });
        await saveLogin(browser, baseURL as string, defaultLogin, storageStatePath);
    });

    test.beforeEach(async ({ browser }) => {
        const context = await browser.newContext({ storageState: storageStatePath });
        const page = await context.newPage();

        generalPage = new GeneralPage(page);
    });

    test('foo', async () => {
        // Test using `generalPage`
    });
});
