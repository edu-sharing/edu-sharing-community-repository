import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { HttpClient, HttpHandler, HttpXhrBackend } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { GenericDialogComponent } from './generic-dialog.component';
import { SharedModule } from '../../../../shared/shared.module';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';

// TODO: Include dialog frame in the stories

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<GenericDialogComponent<string>> = {
    title: 'Dialogs/Generic Dialog',
    component: GenericDialogComponent,
    decorators: [
        moduleMetadata({
            declarations: [],
            imports: [SharedModule],
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
                { provide: CARD_DIALOG_DATA, useValue: {} },
                { provide: CardDialogRef, useValue: {} },
            ],
        }),
    ],
    tags: ['autodocs'],
    // args: {
    //     data: {
    //         // buttons:
    //     }
    // },
    argTypes: {
        // data: {
        //     control: '',
        //     options: ['info', 'warning', 'error'],
        // },
    },
};

export default meta;
type Story = StoryObj<GenericDialogComponent<string>>;

// More on writing stories with args: https://storybook.js.org/docs/angular/writing-stories/args
export const Info: Story = {
    args: {
        data: {
            messageText: 'foo',
        },
    },
};
