import { BrowserPlatformLocation, PlatformLocation } from '@angular/common';
import { Injectable } from '@angular/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { EditorModule } from '@tinymce/tinymce-angular';
import { InitialValues, MdsWidgetType } from 'ngx-edu-sharing-ui';
import { MdsWidget } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../shared/shared.module';
import { RequiredMode } from '../../../types/types';
import { Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetTinyMCEComponent } from './mds-editor-widget-tinymce.component';

/**
 * Test bed for TinyMCE upgrades. The MDS widget merges `configuration` (JSON) over its defaults,
 * so the stories below also replay the editor configs of the other TinyMCE usages
 * (admin messages, editorial assignments) to cover all variants in one place.
 */

/**
 * The storybook iframe has no `<base>` element, so `getBaseHrefFromDOM()` would return `null`
 * and TinyMCE would be loaded from `nullassets/tinymce/...`. Resolve it to the iframe's directory.
 */
@Injectable()
class StorybookPlatformLocation extends BrowserPlatformLocation {
    override getBaseHrefFromDOM(): string {
        return location.pathname.replace(/[^/]*$/, '');
    }
}

/** Widget dummy that keeps the value, so the produced HTML can be inspected below the editor. */
class TinyMceWidgetDummy extends WidgetDummy {
    value: string[] = [];
    constructor(definition: MdsWidget, private initialHtml?: string) {
        super(definition);
        this.value = initialHtml ? [initialHtml] : [];
    }
    getInitialValues(): InitialValues {
        return {
            individualValues: [],
            jointValues: this.initialHtml ? [this.initialHtml] : [],
        };
    }
    setValue(value?: string[]) {
        this.value = value;
    }
}

type TinyMceStoryArgs = Widget['definition'] & { initialHtml?: string };

const SAMPLE_HTML =
    '<h2>Überschrift</h2><p>Ein <strong>fetter</strong>, <em>kursiver</em> und ' +
    '<span style="text-decoration: underline;">unterstrichener</span> Text mit ' +
    '<a href="https://edu-sharing.com" target="_blank" rel="noopener">Link</a>.</p>' +
    '<ul><li>Punkt 1</li><li>Punkt 2</li></ul><p style="text-align: center;">zentriert</p>';

/** mirrors `messages.component.ts` (admin messages) */
const ADMIN_MESSAGES_CONFIG = {
    convert_urls: false,
    plugins: ['link', 'code'],
    toolbar:
        'bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | code | undo redo',
};

/** mirrors `AssignmentEditorConfig` in `manage-assignment.component.ts` (editorial pages) */
const ASSIGNMENT_CONFIG = {
    suffix: '.min',
    plugins: ['lists'],
    default_link_target: '_blank',
    link_title: false,
    newline_behavior: 'invert',
    link_assume_external_targets: true,
    toolbar: 'bold | bullist numlist | undo redo',
};

const meta: Meta<TinyMceStoryArgs> = {
    title: 'Mds/Widget/TinyMCE',
    component: MdsEditorWidgetTinyMCEComponent,
    decorators: [
        moduleMetadata({
            declarations: [MdsEditorWidgetContainerComponent, RegisterFormFieldDirective],
            imports: [SharedModule, EditorModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                { provide: PlatformLocation, useClass: StorybookPlatformLocation },
            ],
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        type: { table: { disable: true } },
        expandable: { table: { disable: true } },
        caption: { control: 'text' },
        bottomCaption: { control: 'text' },
        isRequired: {
            control: 'inline-radio',
            options: [RequiredMode.Mandatory, RequiredMode.Optional, RequiredMode.Ignore],
        },
        configuration: { control: 'text' },
        initialHtml: { control: 'text' },
    },
    args: {
        id: 'ccm:test_html',
        type: MdsWidgetType.TinyMCE,
        caption: 'HTML-Editor',
        expandable: 'disabled',
        isRequired: RequiredMode.Optional,
    },
    render: ({ initialHtml, ...args }) => {
        const widget = new TinyMceWidgetDummy(args as MdsWidget, initialHtml);
        return {
            props: { widget },
            template: `
                <es-mds-editor-widget-checkbox [widget]="widget"></es-mds-editor-widget-checkbox>
                <h4>Ausgabe (setValue)</h4>
                <pre style="white-space: pre-wrap" data-testid="tinymce-output">{{ widget.value?.[0] }}</pre>
            `,
        };
    },
};

export default meta;
type Story = StoryObj<TinyMceStoryArgs>;

/** MDS default config (headings, formatting, link, alignment) */
export const MdsDefault: Story = {};

export const MdsWithContent: Story = {
    args: { initialHtml: SAMPLE_HTML },
};

export const MdsMandatory: Story = {
    args: { isRequired: RequiredMode.Mandatory, bottomCaption: 'Pflichtfeld' },
};

/** config of the admin message editor (`link` + `code` plugin, `convert_urls: false`) */
export const AdminMessagesConfig: Story = {
    args: {
        configuration: JSON.stringify(ADMIN_MESSAGES_CONFIG),
        initialHtml: '<p>Relativer <a href="components/workspace">Link</a> bleibt erhalten.</p>',
    },
};

/** config of the editorial assignment editors (`lists` plugin, inverted newline behavior) */
export const AssignmentConfig: Story = {
    args: {
        configuration: JSON.stringify(ASSIGNMENT_CONFIG),
        initialHtml:
            '<p>Hinweise zur Abgabe:</p><ol><li>Datei hochladen</li><li>Absenden</li></ol>',
    },
};
