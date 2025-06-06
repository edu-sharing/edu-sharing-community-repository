import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsViewerComponent } from './mds-viewer.component';
import { MdsDefinition } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import { mdsStorybookProviders } from '../../../../../src/app/features/mds/mds-editor/storybook-utils';

const DefaultMds = {} as MdsDefinition;
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
    },
};
