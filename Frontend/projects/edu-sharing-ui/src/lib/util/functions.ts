export function notNull<T>(value: T): boolean {
    return value !== undefined && value !== null;
}

export function isTrue(value: boolean): boolean {
    return value ?? false;
}

export function microTick(): Promise<void> {
    return Promise.resolve();
}

export function macroTick(): Promise<void> {
    return new Promise((resolve) => setTimeout(() => resolve()));
}
