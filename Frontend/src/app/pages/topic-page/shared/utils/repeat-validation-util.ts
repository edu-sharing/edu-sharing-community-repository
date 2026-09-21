import { REPEAT_PLACEHOLDERS } from '../types/swimlane-repeat';

/**
 * The outcome of checking what an author wrote into a config patch: either a parsed value, or the
 * i18n key of the first problem found together with the data its message needs.
 */
export interface PatchCheckResult {
    value?: Record<string, unknown>;
    errorKey?: string;
    errorParams?: { [key: string]: string };
}

const PLACEHOLDER_PATTERN = /\$\{([\w.]+)\}/g;
const I18N_PREFIX = 'TOPIC_PAGE.SWIMLANE.EDIT.ADVANCED.ERROR.';

/**
 * Checks the config patch of a grid tile: it has to be a JSON object, and every placeholder it
 * uses has to be one a repeated swimlane can actually resolve. An empty text is valid and means
 * that the tile has no patch.
 *
 * @param text
 */
export function checkConfigPatch(text: string): PatchCheckResult {
    if (!text?.trim()) {
        return {};
    }
    let parsed: unknown;
    try {
        parsed = JSON.parse(text);
    } catch (error) {
        return { errorKey: I18N_PREFIX + 'INVALID_JSON', errorParams: { detail: `${error}` } };
    }
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return { errorKey: I18N_PREFIX + 'NOT_AN_OBJECT' };
    }
    const unknownPlaceholders: string[] = findUnknownPlaceholders(text);
    if (unknownPlaceholders.length > 0) {
        return {
            errorKey: I18N_PREFIX + 'UNKNOWN_PLACEHOLDER',
            errorParams: {
                placeholders: unknownPlaceholders.join(', '),
                allowed: REPEAT_PLACEHOLDERS.join(', '),
            },
        };
    }
    return { value: parsed as Record<string, unknown> };
}

/**
 * The placeholders of a text that no repeat item provides.
 *
 * @param text
 */
export function findUnknownPlaceholders(text: string): string[] {
    const used: string[] = [...text.matchAll(PLACEHOLDER_PATTERN)].map((match) => match[1]);
    return [...new Set(used.filter((path: string) => !REPEAT_PLACEHOLDERS.includes(path)))];
}
