// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `angular-cli.json`.

export const environment = {
  production: false,
  // Set this to `true` to enables console logs with the origin for each change-detection cycle.
  // When doing this, also add an import statement for `zone.js/dist/long-stack-trace-zone`
  // in `polyfills.ts` after the import of `zone.js`.
  traceChangeDetection: false,
};
