// Writes openapi.json - the spec of the frontend api (src/routes/frontend-api.ts), generated
// from the route schemas. Checked in, like the backend's spec: ngx-edu-sharing-api
// (Frontend/projects/edu-sharing-api, `npm run update`) generates its client from it.
//
// Needs no running server, backend or dist: only the api plugin is registered, with stub
// dependencies. `--check` fails instead of writing when the file is not up to date.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import Fastify from 'fastify';
import { registerOpenApi } from '../src/lib/openapi.ts';
import { FRONTEND_API_PATH, frontendApi } from '../src/routes/frontend-api.ts';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const TARGET = path.join(root, 'openapi.json');
/** as the backend's spec (servers: /edu-sharing/rest) */
const CONTEXT_PATH = '/edu-sharing';

const app = Fastify({ logger: false });
await registerOpenApi(app, CONTEXT_PATH);
await app.register(frontendApi, {
    prefix: `${CONTEXT_PATH}${FRONTEND_API_PATH}`,
    languageDefaults: { get: async () => undefined },
});
await app.ready();
const spec = `${JSON.stringify(app.swagger(), null, 2)}\n`;
await app.close();

if (process.argv.includes('--check')) {
    const current = fs.existsSync(TARGET) ? fs.readFileSync(TARGET, 'utf8') : '';
    if (current !== spec) {
        console.error(`${path.relative(process.cwd(), TARGET)} is out of date - run npm run generate:openapi`);
        process.exit(1);
    }
    console.log('openapi.json is up to date');
} else {
    fs.writeFileSync(TARGET, spec);
    console.log(`wrote ${path.relative(process.cwd(), TARGET)}`);
}
