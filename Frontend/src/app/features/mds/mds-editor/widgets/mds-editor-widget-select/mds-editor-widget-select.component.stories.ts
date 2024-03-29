import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../../../../shared/shared.module';
import { MdsWidgetType, RequiredMode } from '../../../types/types';
import { Widget } from '../../mds-editor-instance.service';
import { WidgetDummy, mdsStorybookProviders } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetSelectComponent } from './mds-editor-widget-select.component';

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/Select',
    component: MdsEditorWidgetSelectComponent,
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
            table: { disable: true },
            control: 'select',
            options: [MdsWidgetType.Singleoption],
        },
        placeholder: {
            control: 'text',
        },
        caption: {
            control: 'text',
        },
        bottomCaption: {
            control: 'text',
        },
        isRequired: {
            control: 'inline-radio',
            options: [RequiredMode.Mandatory, RequiredMode.Optional, RequiredMode.Ignore],
        },
    },
    args: {
        id: 'test',
        caption: 'Caption',
        expandable: 'disabled',
        isRequired: RequiredMode.Optional,
        values: [
            { id: 'foo', caption: 'Foo' },
            { id: 'bar', caption: 'Bar' },
            { id: 'long', caption: 'One very very very very very very long label' },
        ],
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
        type: 'singleoption',
    },
};

export const SingleOptionFull: Story = {
    args: {
        type: 'singleoption',
        bottomCaption: 'Bottom Caption',
        placeholder: 'Placeholder...',
    },
};

export const SingleOptionMandatory: Story = {
    args: {
        type: 'singleoption',
        isRequired: 'mandatory',
    },
};
