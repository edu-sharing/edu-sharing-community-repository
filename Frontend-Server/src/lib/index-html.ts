import { randomBytes } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import type { FastifyBaseLogger } from 'fastify';

const NONCE_PLACEHOLDER = '{{ngCspNonce}}';

/**
 * Serves dist/index.html the way NgServlet does: a fresh CSP nonce per request,
 * substituted into every `{{ngCspNonce}}` placeholder.
 *
 * NgServlet gets the nonce from SecurityHeadersFilter, which stores it in a
 * ThreadLocal. Here it is simply generated per request - same 16 random bytes,
 * same base64 encoding.
 */
export class IndexHtml {
    readonly #file: string;
    readonly #watchForChanges: boolean;
    #template: string;
    #mtimeMs: number;

    private constructor(file: string, template: string, mtimeMs: number, watchForChanges: boolean) {
        this.#file = file;
        this.#template = template;
        this.#mtimeMs = mtimeMs;
        this.#watchForChanges = watchForChanges;
    }

    static load(distDir: string, options: { watchForChanges: boolean }): IndexHtml {
        const file = path.join(distDir, 'index.html');
        let stat: fs.Stats;
        try {
            stat = fs.statSync(file);
        } catch (cause) {
            throw new Error(
                `Angular build output not found: ${file}. Run "npm run build" in Frontend/ or set DIST_DIR.`,
                { cause },
            );
        }
        return new IndexHtml(file, fs.readFileSync(file, 'utf8'), stat.mtimeMs, options.watchForChanges);
    }

    /**
     * The `<base href>` is baked into index.html at Angular build time
     * (`ng build --base-href`). A mismatch with CONTEXT_PATH means every relative
     * asset request misses, so it is worth shouting about at startup.
     */
    verifyBaseHref(contextPath: string, log: FastifyBaseLogger): void {
        const match = /<base\s+href="([^"]*)"/i.exec(this.#template);
        const expected = `${contextPath}/`;
        if (match === null) {
            log.warn({ file: this.#file }, 'index.html has no <base href> - cannot verify against CONTEXT_PATH');
            return;
        }
        if (match[1] !== expected) {
            log.error(
                { baseHref: match[1], expected },
                'index.html <base href> does not match CONTEXT_PATH - rebuild the Angular app with a matching --base-href',
            );
        }
    }

    render(): string {
        if (this.#watchForChanges) {
            this.#reloadIfChanged();
        }
        const nonce = randomBytes(16).toString('base64');
        return this.#template.replaceAll(NONCE_PLACEHOLDER, nonce);
    }

    #reloadIfChanged(): void {
        const stat = fs.statSync(this.#file);
        if (stat.mtimeMs !== this.#mtimeMs) {
            this.#template = fs.readFileSync(this.#file, 'utf8');
            this.#mtimeMs = stat.mtimeMs;
        }
    }
}
