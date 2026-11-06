import fs from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';
import zlib from 'node:zlib';
import send from '@fastify/send';
import { LRUCache } from 'lru-cache';

const gzip = promisify(zlib.gzip);
const brotli = promisify(zlib.brotliCompress);

/**
 * Already compressed formats - kept in memory as they are, compressing them again only
 * costs CPU. Everything else gets gzip / brotli variants when they save enough.
 */
const PRECOMPRESSED = /\.(woff2?|png|jpe?g|gif|webp|avif|ico|mp3|mp4|webm|ogg|zip|gz|br|pdf)$/i;

/** the content type @fastify/send would set, so both paths answer alike */
function contentTypeOf(filePath: string): string {
    const type = send.mime.getType(filePath) ?? 'application/octet-stream';
    return send.isUtf8MimeType(type) ? `${type}; charset=utf-8` : type;
}

/** a compressed variant is only kept when it saves at least this much */
const MIN_SAVING = 0.1;

export type Encoding = 'br' | 'gzip' | 'identity';

/** hit: from memory, miss: read (and compressed) just now, bypass: not for the cache (too large, missing, outside the root) */
export type LookupResult = 'hit' | 'miss' | 'bypass';

export interface CachedFile {
    readonly identity: Buffer;
    readonly br?: Buffer;
    readonly gzip?: Buffer;
    readonly contentType: string;
    readonly etag: string;
    readonly lastModified: string;
    /** all variants together - what the entry costs in the cache */
    readonly bytes: number;
}

export interface StaticCacheOptions {
    readonly root: string;
    /** memory budget over all entries and their variants; 0 disables the cache */
    readonly maxBytes: number;
    /** larger files are streamed from disk - the only per file limit, every type is cached */
    readonly maxFileBytes: number;
    /** how long an entry of a file WITHOUT a content hash in its name is trusted before its mtime is checked again */
    readonly revalidateMs: number;
    /** decides which files carry a content hash - those never change */
    readonly isHashed: (fileName: string) => boolean;
}

/**
 * The static files of the app - of any type up to `maxFileBytes` - read (and compressed)
 * once per pod and then answered from memory: no open / read / close through the libuv
 * threadpool and no stream buffers per request - the costliest part of serving the
 * bundles (see docs/04-deployment.md).
 *
 * Bounded in bytes (lru-cache `maxSize`), entries are handed out by reference, never
 * copied. Files with a content hash in their name never change and never expire; the few
 * others are revalidated against their mtime after `revalidateMs`.
 */
export class StaticCache {
    readonly #options: StaticCacheOptions;
    readonly #root: string;
    readonly #entries: LRUCache<string, CachedFile>;
    /** one read + compression per file at a time, however many requests ask */
    readonly #loading = new Map<string, Promise<CachedFile | undefined>>();

    constructor(options: StaticCacheOptions) {
        this.#options = options;
        this.#root = path.resolve(options.root) + path.sep;
        this.#entries = new LRUCache<string, CachedFile>({
            maxSize: Math.max(options.maxBytes, 1),
            sizeCalculation: (entry) => Math.max(entry.bytes, 1),
            // an entry larger than the whole budget is never stored
            maxEntrySize: Math.max(options.maxBytes, 1),
        });
    }

    get enabled(): boolean {
        return this.#options.maxBytes > 0;
    }

    /** for the metrics */
    size(): { bytes: number; entries: number } {
        return { bytes: this.#entries.calculatedSize, entries: this.#entries.size };
    }

    /**
     * @param relativePath the path below the root, as requested (url decoded, no query)
     * @returns the cached file, none when it is not for the cache (type, size, missing
     *          file, outside the root) - the caller then falls back to sendFile
     */
    async get(relativePath: string): Promise<{ result: LookupResult; file?: CachedFile }> {
        if (!this.enabled) {
            return { result: 'bypass' };
        }
        const absolute = path.resolve(this.#root, '.' + path.posix.sep + relativePath);
        if (!absolute.startsWith(this.#root)) {
            return { result: 'bypass' };
        }
        const cached = this.#entries.get(absolute);
        if (cached !== undefined) {
            return { result: 'hit', file: cached };
        }
        let loading = this.#loading.get(absolute);
        if (loading === undefined) {
            loading = this.#load(absolute).finally(() => this.#loading.delete(absolute));
            this.#loading.set(absolute, loading);
        }
        const file = await loading;
        return file === undefined ? { result: 'bypass' } : { result: 'miss', file };
    }

    async #load(absolute: string): Promise<CachedFile | undefined> {
        let stat;
        try {
            stat = await fs.stat(absolute);
        } catch {
            return undefined; // missing: the not-found handling of sendFile takes over
        }
        if (!stat.isFile() || stat.size > this.#options.maxFileBytes) {
            return undefined;
        }
        const identity = await fs.readFile(absolute);
        // off the event loop, in the zlib threadpool - once per file and pod
        const [br, gz] = PRECOMPRESSED.test(absolute)
            ? [undefined, undefined]
            : await Promise.all([
                  brotli(identity, { params: { [zlib.constants.BROTLI_PARAM_QUALITY]: 6, [zlib.constants.BROTLI_PARAM_SIZE_HINT]: identity.length } }),
                  gzip(identity, { level: 6 }),
              ]);
        const worth = (compressed: Buffer | undefined): compressed is Buffer =>
            compressed !== undefined && compressed.length <= identity.length * (1 - MIN_SAVING);
        const entry: CachedFile = {
            identity,
            ...(worth(br) ? { br } : {}),
            ...(worth(gz) ? { gzip: gz } : {}),
            contentType: contentTypeOf(absolute),
            // the same shape @fastify/send uses, so a browser cache stays valid across both paths
            etag: `W/"${stat.size.toString(16)}-${stat.mtime.getTime().toString(16)}"`,
            lastModified: stat.mtime.toUTCString(),
            bytes: identity.length + (worth(br) ? br.length : 0) + (worth(gz) ? gz.length : 0),
        };
        this.#entries.set(absolute, entry, {
            ttl: this.#options.isHashed(path.basename(absolute)) ? 0 : this.#options.revalidateMs,
        });
        return entry;
    }
}

/**
 * The variant for an `Accept-Encoding` header: brotli before gzip, identity when the
 * client takes neither (or excludes them with `q=0`) or the variant was not worth keeping.
 */
export function negotiateEncoding(acceptEncoding: string | undefined, file: CachedFile): Encoding {
    const accepted = new Map<string, number>();
    for (const part of (acceptEncoding ?? '').toLowerCase().split(',')) {
        const [name, ...params] = part.trim().split(';');
        if (!name) {
            continue;
        }
        const q = params.map((p) => p.trim()).find((p) => p.startsWith('q='));
        accepted.set(name, q === undefined ? 1 : Number(q.slice(2)) || 0);
    }
    const allows = (name: string) => (accepted.get(name) ?? accepted.get('*') ?? 0) > 0;
    if (file.br !== undefined && allows('br')) {
        return 'br';
    }
    if (file.gzip !== undefined && allows('gzip')) {
        return 'gzip';
    }
    return 'identity';
}
