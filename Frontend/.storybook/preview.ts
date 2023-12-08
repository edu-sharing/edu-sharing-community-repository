import { HttpClient, HttpHandler, HttpXhrBackend } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { applicationConfig, type Preview } from '@storybook/angular';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
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
    decorators: [
        applicationConfig({
            providers: [
                HttpClient,
                {
                    provide: HttpHandler,
                    useValue: new HttpXhrBackend({ build: () => new XMLHttpRequest() }),
                },
                importProvidersFrom(
                    EduSharingApiModule.forRoot({
                        rootUrl: '/api',
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
