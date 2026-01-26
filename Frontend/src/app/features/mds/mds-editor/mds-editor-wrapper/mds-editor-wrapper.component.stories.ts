import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsEditorWrapperComponent } from './mds-editor-wrapper.component';
import { Data, DummyNode, mdsStorybookProviders, registerMockNode } from '../storybook-utils';
import { SharedModule } from '../../../../shared/shared.module';
import { DEFAULT, Node } from 'ngx-edu-sharing-api';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../mds.module';
import { Helper } from 'ngx-edu-sharing-ui';

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
