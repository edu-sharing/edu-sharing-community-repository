import { TranslateModule } from '@ngx-translate/core';
import { moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../shared.module';
import { InfoMessageComponent } from './info-message.component';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<InfoMessageComponent> = {
    title: 'Shared/Info Message',
    component: InfoMessageComponent,
    decorators: [
        moduleMetadata({
            declarations: [], // Prevent duplicate declaration of InfoMessageComponent
            imports: [SharedModule, TranslateModule.forRoot()],
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
