import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { OptionItem, OptionsHelperDataService, Target } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../shared/shared.module';
import { mdsStorybookProviders } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetActionComponent } from './mds-editor-widget-action.component';

const FEEDBACK_OPTION = 'OPTIONS.MATERIAL_FEEDBACK';

/**
 * Stands in for the options helper of the surrounding page. Only the options listed here are
 * "available" — the widget must not render anything for any other option name.
 */
class OptionsHelperDataServiceMock {
    static readonly options = [
        new OptionItem(FEEDBACK_OPTION, 'chat_bubble', () => alert('Test: option triggered')),
        new OptionItem('OPTIONS.DOWNLOAD', 'cloud_download', () => alert('Test: download')),
    ];

    async getAvailableOptions(target: Target): Promise<OptionItem[]> {
        return OptionsHelperDataServiceMock.options;
    }
}

const meta: Meta<MdsEditorWidgetActionComponent> = {
    title: 'Mds/Editor/Widgets/Action',
    component: MdsEditorWidgetActionComponent,
    decorators: [
        moduleMetadata({
            // MdsModule declares the widget without exporting it, so storybook has to declare
            // it itself — same setup as the other widget stories
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective],
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                { provide: OptionsHelperDataService, useClass: OptionsHelperDataServiceMock },
            ],
        }),
    ],
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<MdsEditorWidgetActionComponent>;

/** name and icon are taken from the option itself */
export const FromOption: Story = {
    args: {
        attributes: { option: FEEDBACK_OPTION },
    },
};

/** `caption` and `icon` attributes of the mds tag win over the option */
export const CaptionAndIconOverride: Story = {
    args: {
        attributes: {
            option: FEEDBACK_OPTION,
            caption: 'Rückmeldung geben',
            icon: 'feedback',
        },
    },
};

/**
 * The mds widget definition acts as a fallback when the tag carries no attributes — this is
 * how `<material_feedback>` used to get its `chat_bubble` icon.
 */
export const CaptionAndIconFromMdsDefinition: Story = {
    args: {
        attributes: { option: FEEDBACK_OPTION },
        widget: {
            definition: { id: 'action', caption: 'From the mds definition', icon: 'rate_review' },
        } as MdsEditorWidgetActionComponent['widget'],
    },
};

/**
 * The option is not in the list the options helper returned — missing toolpermission, missing
 * node permission or wrong scope. Nothing must be rendered.
 */
export const OptionNotAvailable: Story = {
    args: {
        attributes: { option: 'OPTIONS.NOT_AVAILABLE_FOR_THIS_NODE' },
    },
};

/** no `option` attribute at all: misconfigured template, nothing must be rendered */
export const OptionMissing: Story = {
    args: {
        attributes: {},
    },
};
