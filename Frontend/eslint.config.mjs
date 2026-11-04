// Turns every lint error into a warning so that the build is only gated by --max-warnings.
import 'eslint-plugin-only-warn';
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';
import angular from 'angular-eslint';
import storybook from 'eslint-plugin-storybook';

const commonRules = {
    'no-console': ['error', { allow: ['warn', 'error', 'info'] }],
    '@typescript-eslint/no-floating-promises': 'error',
    '@angular-eslint/prefer-standalone': 'off',
    // Components that rely on the eager strategy declare it explicitly, so it is not reported as missing OnPush.
    '@angular-eslint/prefer-on-push-component-change-detection': 'off',
};

/**
 * Lint block for the TypeScript files of one workspace area (the app or one of the projects).
 *
 * @param files glob(s) of the TypeScript files the block applies to
 * @param project tsconfig file(s) that type-aware rules resolve the files against
 * @param directivePrefix allowed attribute-selector prefix(es) of directives
 * @param componentPrefix allowed element-selector prefix(es) of components
 */
function typescriptArea({ files, project, directivePrefix, componentPrefix }) {
    return {
        files,
        extends: [tseslint.configs.base, angular.configs.tsRecommended],
        processor: angular.processInlineTemplates,
        languageOptions: {
            parserOptions: { project, tsconfigRootDir: import.meta.dirname },
        },
        rules: {
            ...commonRules,
            '@angular-eslint/directive-selector': [
                'error',
                { type: 'attribute', prefix: directivePrefix, style: 'camelCase' },
            ],
            '@angular-eslint/component-selector': [
                'error',
                { type: 'element', prefix: componentPrefix, style: 'kebab-case' },
            ],
        },
    };
}

export default defineConfig([
    {
        ignores: [
            '**/node_modules/**',
            '.angular/**',
            'dist/**',
            'projects/rendering-service-lib/dist/**',
            'projects/rendering-service-lib/src/environments/**',
        ],
    },
    {
        // Generated API clients carry a blanket `eslint-disable` header that is not meant to be reported.
        linterOptions: { reportUnusedDisableDirectives: 'off' },
    },
    typescriptArea({
        files: ['src/**/*.ts'],
        project: ['tsconfig.eslint.json'],
        directivePrefix: 'es',
        componentPrefix: 'es',
    }),
    typescriptArea({
        files: ['projects/b-api/**/*.ts'],
        project: ['projects/b-api/tsconfig.lib.json', 'projects/b-api/tsconfig.spec.json'],
        directivePrefix: 'lib',
        componentPrefix: 'lib',
    }),
    typescriptArea({
        files: ['projects/edu-sharing-api/**/*.ts'],
        project: [
            'projects/edu-sharing-api/tsconfig.lib.json',
            'projects/edu-sharing-api/tsconfig.spec.json',
        ],
        directivePrefix: 'lib',
        componentPrefix: 'lib',
    }),
    typescriptArea({
        files: ['projects/rendering-service-api/**/*.ts'],
        project: [
            'projects/rendering-service-api/tsconfig.lib.json',
            'projects/rendering-service-api/tsconfig.spec.json',
        ],
        directivePrefix: 'lib',
        componentPrefix: 'lib',
    }),
    typescriptArea({
        files: ['projects/edu-sharing-ui/**/*.ts'],
        project: [
            'projects/edu-sharing-ui/tsconfig.lib.json',
            'projects/edu-sharing-ui/tsconfig.lint.json',
        ],
        directivePrefix: ['lib', 'es'],
        componentPrefix: ['lib', 'es'],
    }),
    typescriptArea({
        files: ['projects/embed/**/*.ts'],
        project: ['projects/embed/tsconfig.app.json', 'projects/embed/tsconfig.spec.json'],
        directivePrefix: 'app',
        componentPrefix: 'app',
    }),
    typescriptArea({
        files: ['projects/rendering-service-lib/**/*.ts'],
        project: [
            'projects/rendering-service-lib/tsconfig.lib.json',
            'projects/rendering-service-lib/tsconfig.spec.json',
        ],
        directivePrefix: 'es',
        componentPrefix: 'es',
    }),
    typescriptArea({
        files: ['projects/rendering-service-web-component/**/*.ts'],
        project: [
            'projects/rendering-service-web-component/tsconfig.app.json',
            'projects/rendering-service-web-component/tsconfig.spec.json',
        ],
        directivePrefix: 'app',
        componentPrefix: 'edu',
    }),
    {
        files: ['**/*.html'],
        extends: [angular.configs.templateRecommended],
        rules: {},
    },
    storybook.configs['flat/recommended'],
]);
