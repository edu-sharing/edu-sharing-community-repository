import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';

import { PreviewSidebarComponent } from './preview-sidebar.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from 'ngx-edu-sharing-ui';
import { EditorialSidebarModule } from '../editorial-sidebar.module';
import { DummyNode, mdsStorybookProviders, ToastMock } from '../../mds/mds-editor/storybook-utils';

const sidebar: Meta<PreviewSidebarComponent> = {
    title: 'Preview/Sidebar',
    component: PreviewSidebarComponent,
    decorators: [
        moduleMetadata({
            imports: [EditorialSidebarModule, SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat([{ provide: Toast, useClass: ToastMock }]),
        }),
    ],
    args: {
        node: DummyNode as Node,
    },
    argTypes: {},
    tags: ['autodocs'],
};

export default sidebar;
type Story = StoryObj<PreviewSidebarComponent>;

export const NodesMode: Story = {
    args: {
        editorMode: 'nodes',
    },
};

export const ViewerMode: Story = {
    args: {
        editorMode: 'viewer',
    },
};
