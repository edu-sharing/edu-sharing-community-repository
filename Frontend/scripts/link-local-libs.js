// Replaces the npm copies of our own libraries in `node_modules` with links to their local builds
// in `dist`.
//
// Our code imports these libraries via the `paths` in `tsconfig.json`, which resolve to `dist`.
// Third-party packages depending on them (e.g. `ngx-rendering-service-lib`) are resolved through
// `node_modules` instead, where npm installs the published versions as peer dependencies. Without
// this, every bundle would contain two copies of each library with separate injection tokens, so
// e.g. `EduSharingApiModule.forRoot()` would not configure the copy used by the rendering service
// and its requests would go to the default relative `rootUrl`.
//
// Run after the libraries have been built to `dist`.
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const libs = {
    'ngx-edu-sharing-api': 'dist/edu-sharing-api',
    'ngx-edu-sharing-ui': 'dist/edu-sharing-ui',
};

for (const [name, distDir] of Object.entries(libs)) {
    const target = path.join(root, distDir);
    const link = path.join(root, 'node_modules', name);
    if (!fs.existsSync(path.join(target, 'package.json'))) {
        console.error(`${name}: ${distDir} has not been built yet`);
        process.exit(1);
    }
    fs.rmSync(link, { recursive: true, force: true });
    // 'junction' is ignored on other platforms, but does not require admin rights on Windows.
    fs.symlinkSync(target, link, 'junction');
    console.log(`${name}: linked node_modules/${name} -> ${distDir}`);
}
