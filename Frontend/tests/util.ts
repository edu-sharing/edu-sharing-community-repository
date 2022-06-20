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
