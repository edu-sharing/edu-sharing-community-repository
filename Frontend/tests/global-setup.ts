import { chromium, FullConfig } from '@playwright/test';
import { adminLogin, defaultLogin } from './util/constants';
import { saveLogin } from './util/saveLogin';

async function globalSetup(config: FullConfig) {
    const browser = await chromium.launch();
    const baseURL = config.projects[0].use.baseURL as string;
    await Promise.all([
        saveLogin(browser, baseURL, defaultLogin),
        saveLogin(browser, baseURL, adminLogin),
    ]);
    await browser.close();
}

export default globalSetup;
