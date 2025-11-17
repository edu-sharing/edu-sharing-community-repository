import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';

import { PreviewSidebarComponent } from './preview-sidebar.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { Node } from 'ngx-edu-sharing-api';
import { DummyNode, mdsStorybookProviders, ToastMock } from '../mds/mds-editor/storybook-utils';
import { PreviewSidebarModule } from './preview-sidebar.module';
import { Toast } from 'ngx-edu-sharing-ui';
import { ActivatedRoute } from '@angular/router';

const sidebar: Meta<PreviewSidebarComponent> = {
    title: 'Preview/Sidebar',
    component: PreviewSidebarComponent,
    decorators: [
        moduleMetadata({
            imports: [PreviewSidebarModule, SharedModule],
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

export const PreviewStory: Story = {};
