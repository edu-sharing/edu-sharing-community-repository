export interface LoginCredentials {
    username: string;
    password: string;
}

export interface InlineFile {
    name: string;
    mimeType: string;
    buffer: Buffer;
}

export function getStorageStatePath(loginCredentials: LoginCredentials): string {
    return 'playwright/storage/' + loginCredentials.username + '.json';
}

export function generateTestThingName(thing: string): string {
    return (
        `Test ${thing} ` +
        new Date().getTime().toString(36) +
        Math.random().toString(36).substring(7)
    );
}

export function generateTestFile(): InlineFile {
    const fileName = generateTestThingName('file');
    return {
        name: fileName + '.txt',
        mimeType: 'plain/text',
        buffer: Buffer.from(fileName),
    };
}

/** Gets the filename without its extension. */
export function getBaseName(filename: string): string {
    return filename.split('.')[0];
}
