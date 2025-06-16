import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsViewerComponent } from './mds-viewer.component';
import { RestConstants } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import {
    Data,
    DefaultMds,
    mdsStorybookProviders,
} from '../../../../../src/app/features/mds/mds-editor/storybook-utils';

const viewer: Meta<MdsViewerComponent> = {
    title: 'Mds/Viewer',
    component: MdsViewerComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default viewer;
type Story = StoryObj<MdsViewerComponent>;
export const IO_Render_Inline: Story = {
    args: {
        mds: DefaultMds,
        groupId: 'io_render_inline',
        data: Data,
    },
};
export const IO_Render: Story = {
    args: {
        mds: DefaultMds,
        groupId: 'io_render',
        data: Data,
    },
};
