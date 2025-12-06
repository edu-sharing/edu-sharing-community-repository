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
    args: {
        embedded: true,
        setId: DEFAULT,
        editorMode: 'nodes',
        nodes: [DummyNode as Node],
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
