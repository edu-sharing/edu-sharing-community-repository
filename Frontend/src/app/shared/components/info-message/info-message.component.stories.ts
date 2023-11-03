import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { InfoMessageComponent } from './info-message.component';
import { SharedModule } from '../../shared.module';
import { HttpClient, HttpHandler, HttpXhrBackend } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { TranslateModule } from '@ngx-translate/core';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<InfoMessageComponent> = {
    title: 'Shared/Info Message',
    component: InfoMessageComponent,
    decorators: [
        moduleMetadata({
            declarations: [], // Prevent duplicate declaration of InfoMessageComponent
            imports: [SharedModule, TranslateModule.forRoot()],
        }),
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
    ],
    tags: ['autodocs'],
    args: {
        message: 'Your message here!',
    },
    argTypes: {
        mode: {
            control: 'radio',
            options: ['info', 'warning', 'error'],
        },
    },
};

export default meta;
type Story = StoryObj<InfoMessageComponent>;

// More on writing stories with args: https://storybook.js.org/docs/angular/writing-stories/args
export const Info: Story = {
    args: {
        mode: 'info',
    },
};

export const Warning: Story = {
    args: {
        mode: 'warning',
    },
};

export const Error: Story = {
    args: {
        mode: 'error',
    },
};
