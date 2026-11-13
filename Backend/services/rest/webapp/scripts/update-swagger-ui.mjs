// Copies the static Swagger UI from node_modules/swagger-ui-dist into src/main/webapp/swagger.
// Usage: npm install && npm run update-swagger-ui
// swagger-initializer.js is our own config (openapi.json URL) and is never overwritten.
import { copyFileSync, existsSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const src = join(root, 'node_modules', 'swagger-ui-dist');
const target = join(root, 'src', 'main', 'webapp', 'swagger');

const KEEP = new Set(['swagger-initializer.js']);
// npm package metadata / node entry points, not part of the static distribution
const SKIP = new Set(['package.json', 'README.md', 'LICENSE', 'NOTICE', 'index.js', 'absolute-path.js']);

if (!existsSync(src)) {
    console.error('swagger-ui-dist not found, run "npm install" first');
    process.exit(1);
}

// remove previously vendored files (except our own config)
for (const file of readdirSync(target)) {
    if (!KEEP.has(file)) rmSync(join(target, file));
}
for (const file of readdirSync(src)) {
    if (SKIP.has(file) || KEEP.has(file) || /^log\..*\.txt$/.test(file)) continue;
    copyFileSync(join(src, file), join(target, file));
}

const version = JSON.parse(readFileSync(join(src, 'package.json'), 'utf8')).version;
console.log(`swagger-ui-dist ${version} copied to ${target}`);

// report known vulnerabilities; non-zero exit if any are found
const audit = spawnSync('npm', ['audit'], { cwd: root, stdio: 'inherit', shell: process.platform === 'win32' });
process.exit(audit.status ?? 1);
