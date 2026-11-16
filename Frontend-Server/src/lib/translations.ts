import { createHash } from 'node:crypto';
import { promisify } from 'node:util';
import zlib from 'node:zlib';
import type { FastifyBaseLogger } from 'fastify';
import { LRUCache } from 'lru-cache';
import type { Defaults } from './language-defaults.ts';
import type { CachedFile } from './static-cache.ts';
import { applyOverrides, type ResolvedOverrides } from './translation-overrides.ts';

const brotli = promisify(zlib.brotliCompress);
const gzip = promisify(zlib.gzip);

/** the merged translations, in every encoding they are sent in */
export interface TranslationBundle extends Required<Pick<CachedFile, 'identity' | 'br' | 'gzip' | 'etag'>> {
    /** false: built without the overrides (backend unreachable) - not to be cached by the browser */
    readonly complete: boolean;
}

export interface TranslationsOptions {
    readonly defaults: { get(language: string): Promise<Defaults | undefined> };
    readonly overrides: { resolve(language: string, domain: string, contextId?: string): Promise<ResolvedOverrides> };
    /** merged bundles kept at once - per language x context, default 200 */
    readonly maxEntries?: number;
    readonly log: FastifyBaseLogger;
}

/**
 * Level 3 of the translations: the default translations (level 1, per language, part of
 * the image) with the overrides of the client config on top (level 2, per context and
 * config reload), merged, serialized and compressed once - the expensive part, ~15 ms.
 *
 * The key holds the versions of both levels, so a rebuilt level makes a new entry; the
 * old one ages out of the LRU. The ETag is a hash of the content.
 */
export class Translations {
    readonly #options: TranslationsOptions;
    readonly #bundles: LRUCache<string, Promise<TranslationBundle>>;
    readonly #sections: LRUCache<string, Readonly<Record<string, unknown>>>;

    constructor(options: TranslationsOptions) {
        this.#options = options;
        this.#bundles = new LRUCache({ max: options.maxEntries ?? 200 });
        this.#sections = new LRUCache({ max: options.maxEntries ?? 200 });
    }

    /**
     * One top level entry of the merged translations (`SEO`): the default translations with the
     * overrides of the client config on top, as `get` has them - without serializing and compressing
     * the whole bundle, for server side use (the `<head>` of the shell pages).
     *
     * Cached like the bundles, by the versions of both levels. Without the overrides (backend
     * unreachable) the section is built from the defaults alone and not kept.
     *
     * @returns the entry, `{}` if there is none; undefined for a language no scope has a file for
     */
    async section(language: string, domain: string, root: string, contextId?: string): Promise<Readonly<Record<string, unknown>> | undefined> {
        const [defaults, overrides] = await Promise.all([
            this.#options.defaults.get(language),
            this.#options.overrides.resolve(language, domain, contextId),
        ]);
        if (defaults === undefined) {
            return undefined;
        }
        const key = [language, overrides.contextId ?? '', defaults.version, overrides.version, root].join('|');
        const cached = this.#sections.get(key);
        if (cached !== undefined) {
            return cached;
        }
        const prefix = `${root}.`;
        const own = Object.fromEntries(Object.entries(overrides.overrides).filter(([name]) => name.startsWith(prefix)));
        const merged = applyOverrides({ [root]: defaults.translations[root] ?? {} }, own, this.#options.log)[root];
        const section = typeof merged === 'object' && merged !== null && !Array.isArray(merged) ? (merged as Record<string, unknown>) : {};
        if (overrides.complete) {
            this.#sections.set(key, section);
        }
        return section;
    }

    /** undefined for a language no scope has a file for */
    async get(language: string, domain: string, contextId?: string): Promise<TranslationBundle | undefined> {
        const [defaults, overrides] = await Promise.all([
            this.#options.defaults.get(language),
            this.#options.overrides.resolve(language, domain, contextId),
        ]);
        if (defaults === undefined) {
            return undefined;
        }
        const key = [language, overrides.contextId ?? '', defaults.version, overrides.version].join('|');
        let bundle = this.#bundles.get(key);
        if (bundle === undefined) {
            bundle = this.#build(defaults, overrides);
            if (overrides.complete) {
                // a failed build is not kept
                bundle.catch(() => this.#bundles.delete(key));
                this.#bundles.set(key, bundle);
            }
        }
        return bundle;
    }

    async #build(defaults: Defaults, overrides: ResolvedOverrides): Promise<TranslationBundle> {
        const merged = applyOverrides(defaults.translations, overrides.overrides, this.#options.log);
        const identity = Buffer.from(JSON.stringify(merged), 'utf8');
        const [br, gz] = await Promise.all([
            brotli(identity, { params: { [zlib.constants.BROTLI_PARAM_QUALITY]: 6, [zlib.constants.BROTLI_PARAM_SIZE_HINT]: identity.length } }),
            gzip(identity, { level: 6 }),
        ]);
        return {
            identity,
            br,
            gzip: gz,
            // from the content: the same on every pod of a cluster (the versions in the key are per process)
            etag: `"${createHash('sha1').update(identity).digest('base64url').slice(0, 27)}"`,
            complete: overrides.complete,
        };
    }
}
