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
                `Angular build output not found: ${file}. Run "npm run build:frontend" (builds ../Frontend into ../Frontend/dist) and set DIST_DIR=../Frontend/dist in .env, or point DIST_DIR at another build.`,
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

    /**
     * @param nonce of this response - the CSP header must carry the same one, see `newNonce`
     * @param customHead `angular.customHtmlHeaders`, added before the nonce is substituted
     *   (as in NgServlet), so a `{{ngCspNonce}}` inside it is replaced as well
     */
    render(nonce: string, customHead = ''): string {
        if (this.#watchForChanges) {
            this.#reloadIfChanged();
        }
        const template = customHead === '' ? this.#template : insertBeforeHeadEnd(this.#template, customHead);
        return template.replaceAll(NONCE_PLACEHOLDER, nonce);
    }

    /** `SecurityHeadersFilter.generateNonceHash`: 16 random bytes, base64 */
    static newNonce(): string {
        return randomBytes(16).toString('base64');
    }

    #reloadIfChanged(): void {
        const stat = fs.statSync(this.#file);
        if (stat.mtimeMs !== this.#mtimeMs) {
            this.#template = fs.readFileSync(this.#file, 'utf8');
            this.#mtimeMs = stat.mtimeMs;
        }
    }
}

/** `NgServlet.addToHead`: right before the first `</head>`. */
function insertBeforeHeadEnd(html: string, fragment: string): string {
    const position = html.indexOf('</head>');
    return position === -1 ? html : html.slice(0, position) + fragment + html.slice(position);
}
