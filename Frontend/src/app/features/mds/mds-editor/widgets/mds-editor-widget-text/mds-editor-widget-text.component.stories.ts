import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../../../../shared/shared.module';
import { MdsWidgetType, RequiredMode } from '../../../types/types';
import { Widget } from '../../mds-editor-instance.service';
import { WidgetDummy, mdsStorybookProviders } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetTextComponent } from './mds-editor-widget-text.component';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/Text',
    component: MdsEditorWidgetTextComponent,
    decorators: [
        moduleMetadata({
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective],
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        expandable: { table: { disable: true } },
        type: {
            control: 'select',
            options: [
                MdsWidgetType.Text,
                MdsWidgetType.Number,
                MdsWidgetType.Email,
                MdsWidgetType.Date,
                MdsWidgetType.Month,
                MdsWidgetType.Color,
                MdsWidgetType.Textarea,
            ],
        },
        caption: {
            control: 'text',
        },
        bottomCaption: {
            control: 'text',
        },
        placeholder: {
            control: 'text',
        },
        isRequired: {
            control: 'inline-radio',
            options: [RequiredMode.Mandatory, RequiredMode.Optional, RequiredMode.Ignore],
        },
        maxlength: {
            control: 'number',
        },
    },
    args: {
        id: 'test',
        caption: 'Caption',
        expandable: 'disabled',
        isRequired: RequiredMode.Optional,
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

export const TextMinimal: Story = {
    args: {
        type: 'text',
    },
};

export const TextFull: Story = {
    args: {
        type: 'text',
        bottomCaption: 'Bottom Caption',
        placeholder: 'Placeholder...',
    },
};

export const TextMandatory: Story = {
    args: {
        type: 'text',
        isRequired: 'mandatory',
    },
};

export const TextareaMinimal: Story = {
    args: {
        type: 'textarea',
    },
};

export const TextareaFull: Story = {
    args: {
        type: 'textarea',
        bottomCaption: 'Bottom Caption',
        placeholder: 'Placeholder...',
    },
};

export const TextareaMandatory: Story = {
    args: {
        type: 'textarea',
        isRequired: 'mandatory',
    },
};

export const Number: Story = {
    args: {
        type: 'number',
    },
    argTypes: {
        min: {
            control: 'number',
        },
        max: {
            control: 'number',
        },
    },
};

export const Email: Story = {
    args: {
        type: 'email',
    },
};

export const Date: Story = {
    args: {
        type: 'date',
    },
};

export const Month: Story = {
    args: {
        type: 'month',
    },
};

export const Color: Story = {
    args: {
        type: 'color',
    },
};
