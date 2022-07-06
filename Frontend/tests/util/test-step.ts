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
        checkConsoleMessages(page, options),
    ]);
    return result as R; // checkConsoleMessages runs indefinitely and will never win the race.
}

async function checkConsoleMessages(page: Page, options: TestStepOptions): Promise<void> {
    while (true) {
        const msg = await page.waitForEvent('console', { timeout: 0 });
        if (options.failOnConsoleError) {
            expect(msg.type(), 'expect no console error').not.toBe('error');
        }
        if (options.failOnConsoleWarning) {
            expect(msg.type(), 'expect no console warning').not.toBe('warning');
        }
    }
}
