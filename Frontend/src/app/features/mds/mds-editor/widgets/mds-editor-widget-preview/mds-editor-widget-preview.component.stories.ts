import { MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { TranslationsModule } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../shared/shared.module';
import { Widget } from '../../mds-editor-instance.service';
import { WidgetDummy, mdsStorybookProviders } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetPreviewComponent } from './mds-editor-widget-preview.component';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/Preview',
    component: MdsEditorWidgetPreviewComponent,
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
    },
    args: {
        id: 'test',
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

export const Default: Story = {};
