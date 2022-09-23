import type { PlaywrightTestConfig } from '@playwright/test';
import { devices } from '@playwright/test';
import _ from 'lodash';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
require('dotenv').config();

/**
 * Config overrides for running in CI.
 *
 * Activated by environment variable `CI`.
 */
const ciConfig: Partial<PlaywrightTestConfig> = {
    // expect: {
    //     timeout: 10 * 1000,
    // },
    /* Opt out of parallel tests on CI. */
    workers: 1,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: true,
    retries: 2,
    use: {
        actionTimeout: 10 * 1000,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
};

/**
 * Config overrides for developing tests.
 *
 * Activated by environment variable `E2E_TEST_DEV`.
 *
 * Small timeouts for early failure and headed browser.
 *
 * Include
 * ```ts
 * await page.pause()
 * ```
 * in any test to launch the Playwright debugger interface.
 */
const devConfig: Partial<PlaywrightTestConfig> = {
    // Opt out of parallel tests since timeouts are likely to be exceeded
    workers: 1,
    use: {
        actionTimeout: 5 * 1000,
        headless: false,
    },
};

const parallelConfig: Partial<PlaywrightTestConfig> = {
    retries: 2,
};

/**
 * Default configuration.
 *
 * See https://playwright.dev/docs/test-configuration.
 */
const config: PlaywrightTestConfig = {
    testDir: './playwright/out',
    globalSetup: require.resolve('./playwright/out/global-setup'),
    /* Maximum time one test can run for. */
    timeout: 60 * 1000,
    expect: {
        /**
         * Maximum time expect() should wait for the condition to be met. For example in `await
         * expect(locator).toHaveText();`
         */
        // timeout: undefined,
    },

    /* Run tests in files in parallel. */
    /* This causes `beforeAll` and `afterAll` hooks to be executed for each test. */
    fullyParallel: true,
    forbidOnly: false,
    workers: readInt(process.env.E2E_TEST_MAX_WORKERS),
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: [['list'], ['html']],
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        // Tell all tests to load signed-in state from 'storageState.json'.
        // storageState: 'playwright/storageState.json',

        /* Maximum time each action such as `click()` can take. Defaults to 0 (no limit). */
        /* We don't set action timeouts for the default config since parallel runs can increase run
         * times considerably. */
        // actionTimeout: undefined,

        /* Base URL to use in actions like `await page.goto('/')`. */
        baseURL: process.env.E2E_TEST_BASE_URL ?? 'http://localhost:4200/edu-sharing/',

        /* Collect trace. See https://playwright.dev/docs/trace-viewer */
        trace: 'on',
        screenshot: 'on',
        video: 'on',
    },

    /* Configure projects for major browsers */
    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
                ...(readBool(process.env.E2E_TEST_WAYLAND)
                    ? {
                          launchOptions: {
                              args: ['--ozone-platform-hint=auto'],
                          },
                      }
                    : {}),
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

function readInt(s: string): number | undefined {
    const result = parseInt(s, 10);
    if (isNaN(result)) {
        return undefined;
    } else {
        return result;
    }
}

const mode = process.env.E2E_TEST_MODE ?? 'parallel';

if (mode === 'ci' || readBool(process.env.CI)) {
    _.merge(config, ciConfig);
} else if (mode === 'dev') {
    _.merge(config, devConfig);
} else {
    _.merge(config, parallelConfig);
}

export default config;
