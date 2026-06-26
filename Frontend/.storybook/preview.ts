import { provideHttpClient } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { applicationConfig, type Preview } from '@storybook/angular';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { Themes, withTheme } from './theme-wrapper';
import { Variable } from '../src/app/services/theme.service';

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
    lightBlue: {
        primary: '#76a1c0',
        accent: '#76a1c0',
        warn: '#cd2457',
    },
    darkModeColors: {
        primary: '#96cdf8',
        accent: '#96cdf8',
        warn: '#ff6b9d',
    },
};

const preview: Preview = {
    decorators: [
        applicationConfig({
            providers: [
                provideHttpClient(),
                importProvidersFrom(
                    EduSharingApiModule.forRoot({
                        rootUrl: '/api',
                    }),
                ),
                importProvidersFrom(
                    EduSharingUiModule.forRoot({
                        isEmbedded: true,
                        production: false,
                    }),
                ),
            ],
        }),
        withTheme(themes),
    ],
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
        darkMode: {
            description: 'Light / dark color scheme',
            defaultValue: 'light',
            toolbar: {
                title: 'Dark mode',
                icon: 'contrast',
                items: [
                    { value: 'light', title: 'Light' },
                    { value: 'dark', title: 'Dark' },
                ],
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
