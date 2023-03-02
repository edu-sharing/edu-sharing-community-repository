export function notNull<T>(value: T): boolean {
    return value !== undefined && value !== null;
}

export function isTrue(value: boolean): boolean {
    return value ?? false;
}
