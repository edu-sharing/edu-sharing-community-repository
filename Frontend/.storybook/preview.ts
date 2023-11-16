import type { Preview } from '@storybook/angular';
import { Themes, withTheme } from './theme-wrapper';

const themes: Themes = {
    default: {
        primary: '#48708e',
        accent: '#48708e',
        warn: '#cd2457',
    },
    red: {
        primary: '#ff0000',
        accent: '#ff0000',
        warn: '#cd2457',
    },
};

const preview: Preview = {
    decorators: [withTheme(themes)],
    globalTypes: {
        theme: {
            description: 'Material color scheme',
            defaultValue: 'default',
            toolbar: {
                title: 'Theme',
                icon: 'circlehollow',
                items: Object.keys(themes),
            },
        },
    },
    parameters: {
        actions: { argTypesRegex: '^on[A-Z].*' },
        controls: {
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/,
            },
        },
    },
};

export default preview;
