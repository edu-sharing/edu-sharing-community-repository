import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsViewerComponent } from './mds-viewer.component';
import { RestConstants } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import {
    DefaultMds,
    mdsStorybookProviders,
} from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { Values } from '../services/search-helper.service';
import { VCard } from 'ngx-edu-sharing-ui';

const VCardDummy = new VCard();
VCardDummy.givenname = 'Bob';
VCardDummy.surname = 'Test';
const Data: Values = {
    [RestConstants.CM_NAME]: ['Test Name'],
    [RestConstants.LOM_PROP_TITLE]: ['Test Titel'],
    ['cclom:general_keyword']: ['ABC', 'DEF', '123'],
    ['cclom:size']: ['1337'],
    ['cm:created']: [new Date().getTime() + ''],
    ['cm:modified']: [new Date().getTime() + ''],
    ['ccm:lifecyclecontributer_author']: [VCardDummy.toVCardString(), VCardDummy.toVCardString()],
    ['ccm:lifecyclecontributer_publisher']: [
        VCardDummy.toVCardString(),
        VCardDummy.toVCardString(),
    ],
    ['ccm:metadatacontributer_creator']: [VCardDummy.toVCardString()],
    ['ccm:commonlicense_key']: ['CC_0'],
};
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
