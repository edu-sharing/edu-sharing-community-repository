import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsEditorWrapperComponent } from './mds-editor-wrapper.component';
import {
    DummyNode,
    mdsStorybookProviders,
    registerMockNode,
    SuggestionsV1ServiceMock,
} from '../storybook-utils';
import { SharedModule } from '../../../../shared/shared.module';
import { DEFAULT, Node, SuggestionsV1Service } from 'ngx-edu-sharing-api';
import { MdsModule } from '../../mds.module';
import { Helper, MdsExtendedValue, MdsExtendedValues } from 'ngx-edu-sharing-ui';
import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';

function makeUserProposals(
    propertyId: string,
    nodeId: string,
    valueCounts: [string, number][],
): SuggestionResponseDto[] {
    return valueCounts.flatMap(
        ([value, count]) =>
            Array.from({ length: count }, (_, i) => ({
                created: new Date().toISOString(),
                createdBy: { authorityName: `Community User ${i + 1}` },
                propertyId,
                status: 'PENDING',
                version: '1.0',
                id: `proposal-${value}-${i}-${Math.random()}`,
                type: 'USER_PROPOSAL',
                confidence: 1,
                nodeId,
                value,
            })) as SuggestionResponseDto[],
    );
}

@Injectable()
class SuggestionsWithUserProposalsMock extends SuggestionsV1ServiceMock {
    getSuggestionsByNodeId(params: any, context?: any) {
        return super.getSuggestionsByNodeId(params, context).pipe(
            map((result) => {
                result.suggestions['ccm:educationallearningresourcetype'] = [
                    ...(result.suggestions['ccm:educationallearningresourcetype'] ?? []),
                    ...makeUserProposals('ccm:educationallearningresourcetype', params.node, [
                        ['other', 8],
                        ['table', 7],
                        ['experiment', 4],
                        ['graph', 3],
                        ['index', 2],
                    ]),
                ];
                result.suggestions['cclom:general_keyword'] = [
                    ...(result.suggestions['cclom:general_keyword'] ?? []),
                    ...makeUserProposals('cclom:general_keyword', params.node, [
                        ['Mathematik', 5],
                        ['Grundschule', 4],
                        ['Lernspiel', 3],
                        ['Interaktiv', 2],
                    ]),
                ];
                return result;
            }),
        );
    }
}

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
      <es-mds-editor-wrapper #mds
      [embedded]="embedded"
      [setId]="setId"
      [groupId]="groupId"
      [editorMode]="editorMode"
      [nodes]="nodes"
      [currentValues]="currentValues"
      (currentValuesChange)="currentValuesChange"
      (currentValuesExtendedChange)="currentValuesExtendedChange"
      ></es-mds-editor-wrapper>
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
let DummyProps = {} as MdsExtendedValues;
Object.entries(DummyNode.properties).map(([k, v]) => {
    DummyProps[k] = {} as MdsExtendedValue;
    v.forEach((key) => {
        (DummyProps[k] as MdsExtendedValue)[key] = {
            enabled: Math.random() > 0.25,
        };
    });
});
export const MdsValuesStory: Story = {
    args: {
        setId: DEFAULT,
        groupId: 'io',
        editorMode: 'search',
        nodes: null,
        currentValues: DummyProps,
        currentValuesChange: (v) => {},
    },
};
export const MdsIOBulkSidebarTemplate: Story = {
    args: {
        embedded: true,
        setId: DEFAULT,
        editorMode: 'valueSelection',
        groupId: 'io_bulk_sidebar',
        nodes: null,
        currentValues: DummyProps,
        currentValuesChange: (v) => {},
        currentValuesExtendedChange: (v) => {},
    },
};

export const MdsIOWithUserProposals: Story = {
    decorators: [
        applicationConfig({
            providers: [
                { provide: SuggestionsV1Service, useClass: SuggestionsWithUserProposalsMock },
            ],
        }),
    ],
    args: {
        groupId: 'io',
    },
};
