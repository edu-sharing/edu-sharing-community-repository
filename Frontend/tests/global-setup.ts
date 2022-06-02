import { Browser, chromium, FullConfig } from '@playwright/test';

async function saveLogin(
    browser: Browser,
    baseURL: string,
    username: string,
    password: string,
): Promise<void> {
    const page = await browser.newPage();
    await page.goto(baseURL);
    await page.locator('input[name="username"]').fill(username);
    await page.locator('input[type="password"]').fill(password);
    await page.locator('input[type="password"]').press('Enter');
    await page.context().storageState({ path: 'playwright/storage/' + username + '.json' });
}

async function globalSetup(config: FullConfig) {
    const browser = await chromium.launch();
    const baseURL = config.projects[0].use.baseURL;
    await saveLogin(browser, baseURL, 'admin', 'admin');
    await browser.close();
}

export default globalSetup;
