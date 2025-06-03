import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../../../../shared/shared.module';
import { Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetChipsComponent } from './mds-editor-widget-chips.component';
import { RequiredMode } from '../../../types/types';
import { HighlightPipe } from '../mds-editor-widget-tree/mds-editor-widget-tree-core/highlight.pipe';
import { MdsWidgetType } from 'ngx-edu-sharing-ui';

const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/Chips',
    component: MdsEditorWidgetChipsComponent,
    decorators: [
        moduleMetadata({
            declarations: [
                MdsEditorWidgetContainerComponent,
                HighlightPipe,
                RegisterFormFieldDirective,
            ],
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
                MdsWidgetType.MultiValueFixedBadges,
                MdsWidgetType.MultiValueSuggestBadges,
                MdsWidgetType.MultiValueBadges,
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

export const WidgetBadges: Story = {
    args: {
        caption: 'Chips Caption',
        type: 'multivalueBadges',
    },
};
export const WidgetSuggestBadges: Story = {
    args: {
        caption: 'Chips Suggest Caption',
        type: 'multivalueSuggestBadges',
        values: [],
    },
};
export const WidgetFixedBadges: Story = {
    args: {
        caption: 'Chips Caption',
        type: 'multivalueFixedBadges',
    },
};
