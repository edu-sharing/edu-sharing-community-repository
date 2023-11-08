import type { Preview } from '@storybook/angular';
import { componentWrapperDecorator } from '@storybook/angular';

export const withTheme = componentWrapperDecorator(
    (story) =>
        `<es-storybook-material-wrapper [theme]="theme"></es-storybook-material-wrapper>${story}`,
    ({ globals }) => {
        return { theme: globals['theme'] };
    },
);
const preview: Preview = {
    decorators: [
        // withTheme
    ],
    globalTypes: {
        theme: {
            description: 'Global theme for components',
            defaultValue: 'light',
            toolbar: {
                // The label to show for this toolbar item
                title: 'Theme Color',
                icon: 'circlehollow',
                // Array of plain string values or MenuItem shape (see below)
                items: ['edu-blue', 'dark-blue', 'red', 'orange'],
                // Change title based on selected value
                dynamicTitle: true,
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
