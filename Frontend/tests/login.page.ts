import { Page } from '@playwright/test';
import { LoginCredentials } from './util';

export class LoginPage {
    static readonly url = './components/login';

    constructor(private readonly page: Page) {}

    async login({ username, password }: LoginCredentials) {
        await this.page.locator('input[name="username"]').click();
        await this.page.locator('input[name="username"]').fill(username);
        await this.page.locator('input[name="username"]').press('Tab');
        await this.page.locator('input[type="password"]').fill(password);
        await this.page.locator('input[type="password"]').press('Enter');
        await this.page.waitForNavigation();
    }
}
