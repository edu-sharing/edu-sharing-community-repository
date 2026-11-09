import fs from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';
import zlib from 'node:zlib';
import type { CachedFile } from './static-cache.ts';

const brotli = promisify(zlib.brotliCompress);
const gzip = promisify(zlib.gzip);

/** a bundle in every encoding it is sent in */
export type Bundle = Required<Pick<CachedFile, 'identity' | 'br' | 'gzip'>>;

/**
 * What the app sends in `X-Edu-Sharing-Language`: `de`, `en`, `de-informal`, `de-no-binnen-i` ...
 * Checked before the value goes anywhere near the file system.
 */
const LANGUAGE = /^[a-z]{2}(-[a-z0-9]+)*$/;

/**
 * The merge order: alphabetical. The scopes have no top level key in common, so the order
 * does not change the result - it only makes it independent of the file system
 * (`I18nAngular.getLanguageStrings` merged in directory listing order).
 */
export function scopeOrder(scopes: readonly string[]): string[] {
    return [...scopes].sort((a, b) => a.localeCompare(b));
}

export function isLanguage(language: string): boolean {
    return LANGUAGE.test(language);
}

/**
 * `GET /rest/config/v1/language/defaults`, port of `I18nAngular.getLanguageStrings`: the
 * `<language>.json` of every scope directory below `assets/i18n`, top level keys merged
 * into one object. The files are part of the image, so every bundle is built once.
 *
 * Differences to the backend, on purpose:
 * - a scope without the language is skipped for every language. The backend did this only
 *   for `de-*` and answered 500 otherwise (`fr` / `it`: `editorial` has no file).
 * - a fixed merge order, see `scopeOrder`.
 */
export class LanguageDefaults {
    readonly #dir: string;
    readonly #revalidate: boolean;
    readonly #bundles = new Map<string, { signature: string; bundle: Promise<Bundle | undefined> }>();

    /**
     * @param i18nDir `dist/assets/i18n`
     * @param revalidate true: check the files (size, mtime) on every request and rebuild
     *   a bundle whose files changed - dev, where the mounted build changes. A few stats
     *   instead of reading, merging and compressing ~250 KB per request.
     */
    constructor(i18nDir: string, { revalidate }: { revalidate: boolean }) {
        this.#dir = i18nDir;
        this.#revalidate = revalidate;
    }

    /**
     * The bundle, undefined for a language no scope has a file for - the caller hands
     * those to the backend, which answers as before.
     */
    async get(language: string): Promise<Bundle | undefined> {
        if (!isLanguage(language)) {
            return undefined;
        }
        const signature = this.#revalidate ? await this.#signature(language) : '';
        const cached = this.#bundles.get(language);
        if (cached !== undefined && cached.signature === signature) {
            return cached.bundle;
        }
        const entry = {
            signature,
            // a failed build (unreadable file) is not kept, the next request tries again
            bundle: this.#build(language).catch((err: unknown) => {
                if (this.#bundles.get(language) === entry) {
                    this.#bundles.delete(language);
                }
                throw err;
            }),
        };
        this.#bundles.set(language, entry);
        return entry.bundle;
    }

    async #scopes(): Promise<string[]> {
        const entries = await fs.readdir(this.#dir, { withFileTypes: true });
        return scopeOrder(entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name));
    }

    /** size and mtime of every `<scope>/<language>.json` - changes with any file of the bundle */
    async #signature(language: string): Promise<string> {
        const scopes = await this.#scopes();
        const stats = await Promise.all(
            scopes.map((scope) => fs.stat(path.join(this.#dir, scope, `${language}.json`)).then(
                (stat) => `${scope}:${stat.size}:${stat.mtimeMs}`,
                () => `${scope}:-`,
            )),
        );
        return stats.join('|');
    }

    /** the merged object, undefined if no scope has the language */
    async merge(language: string): Promise<Record<string, unknown> | undefined> {
        if (!isLanguage(language)) {
            return undefined;
        }
        const scopes = await this.#scopes();
        const result: Record<string, unknown> = {};
        let found = false;
        for (const scope of scopes) {
            let text: string;
            try {
                text = await fs.readFile(path.join(this.#dir, scope, `${language}.json`), 'utf8');
            } catch (err) {
                if ((err as NodeJS.ErrnoException).code === 'ENOENT') {
                    continue;
                }
                throw err;
            }
            found = true;
            if (text.trim() === '') {
                continue;
            }
            const parsed: unknown = JSON.parse(text);
            if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
                Object.assign(result, parsed);
            }
        }
        return found ? result : undefined;
    }

    async #build(language: string): Promise<Bundle | undefined> {
        const merged = await this.merge(language);
        if (merged === undefined) {
            return undefined;
        }
        const identity = Buffer.from(JSON.stringify(merged), 'utf8');
        const [br, gz] = await Promise.all([
            brotli(identity, { params: { [zlib.constants.BROTLI_PARAM_QUALITY]: 6, [zlib.constants.BROTLI_PARAM_SIZE_HINT]: identity.length } }),
            gzip(identity, { level: 6 }),
        ]);
        return { identity, br, gzip: gz };
    }
}
