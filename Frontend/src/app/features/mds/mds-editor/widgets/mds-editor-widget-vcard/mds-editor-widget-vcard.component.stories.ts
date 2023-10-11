import { MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { TranslationsModule } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../shared/shared.module';
import { MdsWidgetType } from '../../../types/types';
import { Widget } from '../../mds-editor-instance.service';
import { WidgetDummy, mdsStorybookProviders } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetVCardComponent } from './mds-editor-widget-vcard.component';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/VCard',
    component: MdsEditorWidgetVCardComponent,
    decorators: [
        moduleMetadata({
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective],
            imports: [SharedModule, MatSnackBarModule, TranslateModule, TranslationsModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        expandable: { table: { disable: true } },
        type: {
            table: { disable: true },
            control: 'select',
            options: [MdsWidgetType.VCard],
        },
        caption: {
            control: 'text',
        },
        bottomCaption: {
            control: 'text',
        },
    },
    args: {
        id: 'test',
        caption: 'Caption',
        expandable: 'disabled',
    },
    tags: ['autodocs'],
    render: (args) => {
        return {
            props: {
                widget: new WidgetDummy(args) as unknown as Widget,
            },
        };
    },
};

export default meta;
type Story = StoryObj<Widget['definition']>;

export const SingleOptionMinimal: Story = {
    args: {
        type: 'vcard',
    },
};
