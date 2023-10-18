import { Injector, importProvidersFrom } from '@angular/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { provideAnimations } from '@angular/platform-browser/animations';
import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { injectInjectorToProps } from '../../../../storybook/custom-decorators';
import { SharedModule } from '../../../shared/shared.module';
import { Toast } from '../../toast';
import { ToastMessageComponent } from './toast-message.component';

interface ToastStories {
    message: string;
    action?: string;
    duration: number;
    type: 'info' | 'error';
}

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<ToastStories> = {
    title: 'Core UI/Toast Message',
    decorators: [
        moduleMetadata({
            declarations: [ToastMessageComponent],
            imports: [MatSnackBarModule, SharedModule],
        }),
        applicationConfig({
            providers: [provideAnimations(), importProvidersFrom(MatSnackBarModule)],
        }),
        injectInjectorToProps(),
    ],
    tags: ['autodocs'],
    args: {
        message: 'Toast!',
        duration: 3,
        type: 'info',
        action: 'Done!',
    },
    argTypes: {
        type: {
            control: 'inline-radio',
            options: ['info', 'error'],
        },
    },
    render: (props) => ({
        props: {
            showToast: (injector: Injector) => {
                injector.get(MatSnackBar).openFromComponent(ToastMessageComponent, {
                    data: {
                        message: props.message,
                        type: props.type,
                        action: props.action
                            ? { label: props.action, callback: () => void 0 }
                            : null,
                    },
                    duration: Toast.convertDuration(props.duration * 1000),
                    panelClass: ['toast-message', `toast-message-${props.type}`],
                });
            },
        },
        template: `
          <button (click)="showToast(injector)">Show Toast</button>
        `,
    }),
};

export default meta;
type Story = StoryObj<ToastStories>;

// More on writing stories with args: https://storybook.js.org/docs/angular/writing-stories/args
export const Info: Story = {};

export const InfoLongMessage: Story = {
    args: {
        message:
            'This is a long long long long long long long long long long long long long long long toast message.',
        duration: 8,
    },
};

export const InfoLongMessageLongAction: Story = {
    args: {
        message:
            'This is a long long long long long long long long long long long long long long long toast message.',
        duration: 8,
        action: 'This is a long long long long long long action label',
    },
};

export const Error: Story = {
    args: {
        type: 'error',
    },
};
