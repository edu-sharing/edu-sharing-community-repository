export interface LoginCredentials {
    username: string;
    password: string;
}

export function getStorageStatePath(loginCredentials: LoginCredentials): string {
    return 'playwright/storage/' + loginCredentials.username + '.json';
}

export function generateTestThingName(thing: string): string {
    return `Test ${thing} ${new Date().getTime()}`;
}

export function getBaseName(filename: string): string {
    return filename.split('.')[0];
}

// Cannot use since Playwright doesn't support decorators.
//
// export function testStep() {
//     return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
//         const originalFunction = descriptor.value;
//         descriptor.value = async function (this: any, ...args: any[]) {
//             await test.step(propertyKey, async () => {
//                 await originalFunction.apply(this, args);
//             });
//         };
//     };
// }
