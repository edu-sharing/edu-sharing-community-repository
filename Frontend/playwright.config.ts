import type { PlaywrightTestConfig } from '@playwright/test';
import { devices } from '@playwright/test';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
require('dotenv').config();

const CI = readBool(process.env.CI);
const E2E_TEST_DEV = readBool(process.env.E2E_TEST_DEV);

/**
 * See https://playwright.dev/docs/test-configuration.
 */
const config: PlaywrightTestConfig = {
    testDir: './tests',
    globalSetup: require.resolve('./tests/global-setup'),
    /* Maximum time one test can run for. */
    timeout: 30 * 1000,
    expect: {
        /**
         * Maximum time expect() should wait for the condition to be met.
         * For example in `await expect(locator).toHaveText();`
         */
        timeout: E2E_TEST_DEV ? 5 * 1000 : undefined,
    },

    /* Run tests in files in parallel. */
    /* This causes `beforeAll` and `afterAll` hooks to be executed for each test. */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: CI,
    /* Retry on CI only */
    retries: CI ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    /* Also on E2E_TEST_DEV, since action- and expect timeouts will be exceeded with parallel runs. */
    workers: CI || E2E_TEST_DEV ? 1 : undefined,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: [['list'], ['html']],
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        // Tell all tests to load signed-in state from 'storageState.json'.
        // storageState: 'playwright/storageState.json',

        /* Maximum time each action such as `click()` can take. Defaults to 0 (no limit). */
        actionTimeout: E2E_TEST_DEV ? 3 * 1000 : undefined,

        /* Base URL to use in actions like `await page.goto('/')`. */
        baseURL: 'http://localhost:4200/edu-sharing/',

        /* Collect trace. See https://playwright.dev/docs/trace-viewer */
        trace: (() => {
            if (E2E_TEST_DEV) return 'on';
            else if (CI) return 'on-first-retry';
            else return 'retain-on-failure';
        })(),

        headless: !E2E_TEST_DEV,
    },

    /* Configure projects for major browsers */
    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
                ...[
                    readBool(process.env.E2E_TEST_WAYLAND)
                        ? {
                              launchOptions: {
                                  args: ['--ozone-platform-hint=auto'],
                              },
                          }
                        : {},
                ],
            },
        },

        {
            name: 'firefox',
            use: {
                ...devices['Desktop Firefox'],
            },
        },

        // {
        //   name: 'webkit',
        //   use: {
        //     ...devices['Desktop Safari'],
        //   },
        // },

        /* Test against mobile viewports. */
        // {
        //   name: 'Mobile Chrome',
        //   use: {
        //     ...devices['Pixel 5'],
        //   },
        // },
        // {
        //   name: 'Mobile Safari',
        //   use: {
        //     ...devices['iPhone 12'],
        //   },
        // },

        /* Test against branded browsers. */
        // {
        //   name: 'Microsoft Edge',
        //   use: {
        //     channel: 'msedge',
        //   },
        // },
        // {
        //   name: 'Google Chrome',
        //   use: {
        //     channel: 'chrome',
        //   },
        // },
    ],

    /* Folder for test artifacts such as screenshots, videos, traces, etc. */
    // outputDir: 'test-results/',

    /* Run your local dev server before starting the tests */
    // webServer: {
    //   command: 'npm run start',
    //   port: 3000,
    // },
};

function readBool(s: string): boolean {
    return ['1', 'true', 'yes'].includes(s?.toLowerCase());
}

export default config;
