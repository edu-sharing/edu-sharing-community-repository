import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../../../../shared/shared.module';
import { Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { RequiredMode } from '../../../types/types';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MdsEditorWidgetRadioButtonComponent } from './mds-editor-widget-radio-button.component';

const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/RadioButton',
    component: MdsEditorWidgetRadioButtonComponent,
    decorators: [
        moduleMetadata({
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective],
            imports: [MatSnackBarModule, SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        expandable: {
            control: 'inline-radio',
            options: ['disabled', 'expanded', 'collapsed'],
        },
        caption: {
            control: 'text',
        },
        bottomCaption: {
            control: 'text',
        },
    },
    args: {
        caption: 'Caption',
        expandable: 'disabled',
        isRequired: RequiredMode.Optional,
    },
    tags: ['autodocs'],
    render: (args, context) => {
        const widget = new WidgetDummy(args) as unknown as Widget;
        (window as any).widget = widget;
        return {
            props: {
                widget,
            },
        };
    },
};

export default meta;
type Story = StoryObj<Widget['definition']>;

export const RadioVerticalDefault: Story = {
    args: {
        caption: 'Radio Vertical Default',
        type: 'radioVertical',
    },
};
export const RadioVerticalExpandable: Story = {
    args: {
        caption: 'Radio Vertical Expandable',
        expandable: 'expanded',
        type: 'radioVertical',
    },
};
export const RadioHorizontalDefault: Story = {
    args: {
        caption: 'Radio Horizontal Default',
        type: 'radioHorizontal',
    },
};
