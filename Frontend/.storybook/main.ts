import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
    stories: [
        '../src/**/*.mdx',
        '../src/**/*.stories.@(js|jsx|mjs|ts|tsx)',
        '../projects/edu-sharing-ui/**/*.stories.@(js|jsx|mjs|ts|tsx)',
    ],
    addons: ['@storybook/addon-links', '@storybook/addon-a11y'],
    framework: {
        name: '@storybook/angular',
        options: {},
    },
    webpackFinal: async (config) => {
        // @angular-devkit/build-angular bundles its own webpack. When Storybook
        // runs angular-devkit's SourceMapDevToolPlugin against the root webpack
        // Compiler, the `instanceof Compilation` check fails across instances.
        // Use the built-in devtool instead.
        config.devtool = 'inline-source-map';
        config.plugins = config.plugins?.filter(
            (p: any) => !p.constructor.name.includes('SourceMapDevToolPlugin'),
        );
        return config;
    },
};
export default config;
