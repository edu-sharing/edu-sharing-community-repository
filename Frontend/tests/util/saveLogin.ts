import { Browser, request } from '@playwright/test';
import * as fs from 'fs';
import { getStorageStatePath, LoginCredentials } from './util';

export async function saveLogin(
    browser: Browser,
    baseURL: string,
    credentials: LoginCredentials,
    customStoragePath?: string,
): Promise<void> {
    // Reuse existing session if not older than 10 minutes
    const storageStatePath = customStoragePath ?? getStorageStatePath(credentials);
    if (await fileExistsAndIsNoOlderThan(storageStatePath, 600)) {
        return;
    }
    // Login
    const page = await browser.newPage();
    await page.goto(baseURL);
    await page.locator('input[name="username"]').fill(credentials.username);
    await page.locator('input[type="password"]').fill(credentials.password);
    await Promise.all([
        page.locator('input[type="password"]').press('Enter'),
        page.waitForNavigation(),
    ]);
    // Disable tutorials
    await page.evaluate(() => {
        window.localStorage.setItem('TUTORIAL.USER_TUTORIAL_HEADING', 'true');
        window.localStorage.setItem('TUTORIAL.SEARCH.TUTORIAL_HEADING', 'true');
    });
    // Save session to file
    const storageState = await page.context().storageState({ path: storageStatePath });
    // (Re)set user preferences
    const context = await request.newContext({ baseURL, storageState });
    await context.put('./rest/iam/v1/people/-home-/-me-/preferences', {
        data: {
            language: 'en',
            accessibility_toastDuration: 1,
        },
    });
    await page.close();
}

async function fileExistsAndIsNoOlderThan(path: string, seconds: number): Promise<boolean> {
    try {
        const stat = await fs.promises.lstat(path);
        const diffMs = new Date().getTime() - stat.ctime.getTime();
        return diffMs <= seconds * 1000;
    } catch (e: any) {
        if (e.code === 'ENOENT') {
            return false;
        } else {
            throw e;
        }
    }
}
