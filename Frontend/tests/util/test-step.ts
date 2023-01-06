import { expect, Page, test } from '@playwright/test';

class TestStepOptions {
    failOnConsoleError = true;
    failOnConsoleWarning = false;
}

export function testStep(options: Partial<TestStepOptions> = {}) {
    return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        const originalFunction = descriptor.value;
        descriptor.value = async function (this: any, ...args: any[]) {
            await test.step(this.constructor.name + '.' + propertyKey, async () => {
                await testStepWrapper(this.page, () => originalFunction.apply(this, args), {
                    ...new TestStepOptions(),
                    ...options,
                });
            });
        };
    };
}

async function testStepWrapper<R>(
    page: Page,
    testFunction: () => Promise<R>,
    options: TestStepOptions,
): Promise<R> {
    const result = await Promise.race([
        (async () => {
            // await page.waitForLoadState('networkidle');
            return testFunction();
        })(),
        ...(options.failOnConsoleError ? [checkConsoleErrors(page)] : []),
        ...(options.failOnConsoleWarning ? [checkConsoleWarnings(page)] : []),
    ]);
    return result as R; // checkConsoleMessages runs indefinitely and will never win the race.
}

async function checkConsoleErrors(page: Page): Promise<void> {
    const msg = await page.waitForEvent('console', {
        timeout: 0,
        predicate: (message) =>
            message.type() === 'error' &&
            // These are Playwright errors that are probably caused by us racing page navigation
            // with this console message listener.
            !['JSHandle@object', 'ERROR JSHandle@object', 'ERROR Error'].includes(message.text()),
    });
    expect(msg.text() ?? '<empty message>', `expect no console error`).not.toBeDefined();
}

async function checkConsoleWarnings(page: Page): Promise<void> {
    const msg = await page.waitForEvent('console', {
        timeout: 0,
        predicate: (message) => message.type() === 'warning',
    });
    expect(msg.text() ?? '<empty message>', `expect no console warning`).not.toBeDefined();
}
