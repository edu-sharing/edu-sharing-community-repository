import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsEditorWrapperComponent } from './mds-editor-wrapper.component';
import { DummyNode, mdsStorybookProviders } from '../storybook-utils';
import { SharedModule } from '../../../../shared/shared.module';
import { DEFAULT, Node } from 'ngx-edu-sharing-api';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../mds.module';

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
            delete n.properties['cclom:title'];
            delete n.properties['cclom:general_description'];
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
