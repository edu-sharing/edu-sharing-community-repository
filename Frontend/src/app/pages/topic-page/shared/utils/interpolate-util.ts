/**
 * Scope a template value is interpolated against. Nested objects are addressed with a dot, so
 * `{ item: { title: 'Optik' } }` resolves `${item.title}`.
 */
export type InterpolationScope = { [key: string]: unknown };

// a single placeholder: `${`, a dot-separated path of word characters, `}`
const PLACEHOLDER_PATTERN = /\$\{([\w.]+)\}/g;

/**
 * Resolves a dot-separated path against a scope.
 *
 * @param path
 * @param scope
 */
function resolvePath(path: string, scope: InterpolationScope): unknown {
    return path
        .split('.')
        .reduce<unknown>(
            (value, segment) =>
                value && typeof value === 'object'
                    ? (value as InterpolationScope)[segment]
                    : undefined,
            scope,
        );
}

/**
 * Replaces every placeholder in a string. A path the scope does not cover yields an empty string,
 * so an incomplete scope cannot leak `${…}` into persisted configuration.
 *
 * @param text
 * @param scope
 */
function interpolateString(text: string, scope: InterpolationScope): string {
    return text.replace(PLACEHOLDER_PATTERN, (_match, path: string) => {
        const value: unknown = resolvePath(path, scope);
        return value === null || value === undefined ? '' : String(value);
    });
}

/**
 * Deep-copies a value with every `${…}` placeholder in its strings resolved against the scope.
 *
 * The pattern requires the `${…}` delimiters, so the `{{node(…)}}` tags of AI-generated widget
 * texts pass through untouched.
 *
 * @param value
 * @param scope
 */
export function interpolate<T>(value: T, scope: InterpolationScope): T {
    if (typeof value === 'string') {
        return interpolateString(value, scope) as unknown as T;
    }
    if (Array.isArray(value)) {
        return value.map((entry) => interpolate(entry, scope)) as unknown as T;
    }
    if (value && typeof value === 'object') {
        return Object.fromEntries(
            Object.entries(value).map(([key, entry]) => [key, interpolate(entry, scope)]),
        ) as T;
    }
    return value;
}

/**
 * Whether a value still holds an unresolved placeholder anywhere inside it.
 *
 * @param value
 */
export function containsPlaceholder(value: unknown): boolean {
    return new RegExp(PLACEHOLDER_PATTERN.source).test(JSON.stringify(value ?? ''));
}
