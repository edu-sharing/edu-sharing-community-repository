export function notNull<T>(value: T): boolean {
    return value !== undefined && value !== null;
}
