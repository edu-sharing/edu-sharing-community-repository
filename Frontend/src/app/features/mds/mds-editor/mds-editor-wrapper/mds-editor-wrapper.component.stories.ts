import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsEditorWrapperComponent } from './mds-editor-wrapper.component';
import {
    Data,
    DefaultMds,
    DummyNode,
    mdsStorybookProviders,
    registerMockNode,
} from '../storybook-utils';
import { SharedModule } from '../../../../shared/shared.module';
import { DEFAULT, MdsDefinition, MdsIdentifier, MdsService, Node } from 'ngx-edu-sharing-api';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../mds.module';
import { Helper, OptionItem, OptionsHelperDataService, Target } from 'ngx-edu-sharing-ui';
import { Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';

const meta: Meta<MdsEditorWrapperComponent> = {
    title: 'Mds/Editor',
    component: MdsEditorWrapperComponent,
    decorators: [
        moduleMetadata({
            declarations: [],
            imports: [MdsModule, SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    render: (args) => ({
        props: {
            ...args,
            save: async (mds: MdsEditorWrapperComponent) =>
                alert(JSON.stringify(await mds.onSave(), null, 2)), // <-- your callback
        },
        template: `
      <es-mds-editor-wrapper #mds [embedded]="embedded" [setId]="setId" [groupId]="groupId" [editorMode]="editorMode" [nodes]="nodes"></es-mds-editor-wrapper>
      <button mat-flat-button color="primary" (click)="save(mds)">Test: Save</button>
    `,
    }),
    args: {
        embedded: true,
        setId: DEFAULT,
        editorMode: 'nodes',
        nodes: [DummyNode as Node].map((n) => {
            n = Helper.deepCopy(n);
            n.ref.id = Math.random() + '';
            delete n.properties['cclom:title'];
            delete n.properties['cclom:general_description'];
            console.log(n.properties);
            registerMockNode(n);
            return n;
        }),
    },
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<MdsEditorWrapperComponent>;
export const MdsIOTemplate: Story = {
    args: {
        groupId: 'io',
    },
};
export const MdsIOBulkTemplate: Story = {
    args: {
        groupId: 'io_bulk',
        nodes: [DummyNode as Node, DummyNode as Node].map((n, i) => {
            n = Helper.deepCopy(n);
            n.ref.id = Math.random() + '';
            n.properties['ccm:tool_category'] = ['communication'];
            n.properties['ccm:educationallearningresourcetype'] = [
                'figure',
                'diagram',
                'slide',
                'graph',
                'index',
                'narrativetext',
            ];
            n.properties['ccm:educationaltypicalagerange_from'] = ['10'];
            n.properties['ccm:educationaltypicalagerange_to'] = ['20'];
            const keywords = n.properties['cclom:general_keyword'];
            if (i == 1) {
                keywords.splice(1, 1);
                n.properties['ccm:taxonid'].splice(1, 3);
                n.properties['ccm:educationallearningresourcetype'].splice(1, 2);
                n.properties['cclom:title'] = ['Test 2'];
            }
            registerMockNode(n);
            return n;
        }),
    },
};

const FEEDBACK_OPTION = 'OPTIONS.MATERIAL_FEEDBACK';
const TEMPLATE_GROUP = 'storybook_template_features';

/**
 * View exercising the two template features that are resolved by `MdsEditorViewComponent`
 * itself: `<i18n …>` tags and the generic `<action>` widget.
 */
const TEMPLATE_FEATURES_HTML = `
    <h3><i18n MDS.LICENSE></h3>
    <p>Unbekannter Key bleibt stehen: <i18n MDS.NO_SUCH_KEY></p>
    <cclom:title>
    <action option="${FEEDBACK_OPTION}">
    <action option="${FEEDBACK_OPTION}" caption="Rückmeldung geben" icon="feedback">
    <action option="OPTIONS.NOT_AVAILABLE_FOR_THIS_NODE">
`;

const TemplateFeaturesMds: MdsDefinition = {
    ...DefaultMds,
    views: [
        ...DefaultMds.views,
        {
            id: TEMPLATE_GROUP,
            caption: 'Template-Features',
            icon: 'description',
            html: TEMPLATE_FEATURES_HTML,
            rel: null,
            hideIfEmpty: false,
            isExtended: false,
        },
    ],
    groups: [
        ...DefaultMds.groups,
        { id: TEMPLATE_GROUP, views: [TEMPLATE_GROUP], rendering: 'angular' },
    ],
};

@Injectable()
class TemplateFeaturesMdsService {
    getMetadataSet(_identifier: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return of(TemplateFeaturesMds);
    }
}

/** only `OPTIONS.MATERIAL_FEEDBACK` is available, so the third `<action>` must stay invisible */
@Injectable()
class OptionsHelperDataServiceMock {
    async getAvailableOptions(_target: Target): Promise<OptionItem[]> {
        return [
            new OptionItem(FEEDBACK_OPTION, 'chat_bubble', () => alert('Test: option triggered')),
        ];
    }
}

/**
 * `<i18n KEY>` is replaced with the frontend translation before the template html is parsed,
 * and `<action option="…">` renders the matching option of the options helper.
 */
export const TemplateFeatures: Story = {
    decorators: [
        applicationConfig({
            providers: [
                { provide: MdsService, useClass: TemplateFeaturesMdsService },
                { provide: OptionsHelperDataService, useClass: OptionsHelperDataServiceMock },
            ],
        }),
    ],
    args: {
        groupId: TEMPLATE_GROUP,
        editorMode: 'viewer',
    },
};
