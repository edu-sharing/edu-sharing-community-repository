import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { SharedModule } from '../../../../../shared/shared.module';
import { Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { RequiredMode } from '../../../types/types';
import { MdsEditorWidgetTreeComponent } from './mds-editor-widget-tree.component';
import { HighlightPipe } from './mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MdsWidgetType } from 'ngx-edu-sharing-ui';

const meta: Meta<Widget['definition']> = {
    title: 'Mds/Widget/Tree',
    component: MdsEditorWidgetTreeComponent,
    decorators: [
        moduleMetadata({
            declarations: [
                MdsEditorWidgetContainerComponent,
                MdsEditorWidgetTreeCoreComponent,
                HighlightPipe,
                RegisterFormFieldDirective,
            ],
            imports: [MatSnackBarModule, SharedModule],
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
                MdsWidgetType.MultiValueTree,
                MdsWidgetType.SingleValueTree,
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

export const TreeMultivalue: Story = {
    args: {
        caption: 'Tree Caption',
        type: 'multivalueTree',
    },
};

export const SuggestBadgesMultivalue: Story = {
    args: {
        caption: 'Badges Caption',
        type: 'multivalueSuggestBadges',
        values: [],
    },
};

export const FixedBadgesMultivalue: Story = {
    args: {
        caption: 'Badges Caption',
        type: 'multivalueFixedBadges',
    },
};
