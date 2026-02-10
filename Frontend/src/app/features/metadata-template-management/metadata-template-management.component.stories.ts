import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { SessionStorageService } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../shared/shared.module';
import { DialogsService } from '../dialogs/dialogs.service';
import { MdsModule } from '../mds/mds.module';
import {
    mdsStorybookProviders,
    MetadataTemplateDialogsServiceMock,
    SessionStorageServiceMock,
} from '../mds/mds-editor/storybook-utils';
import { MetadataTemplateManagementComponent } from './metadata-template-management.component';

const additionalProviders = [
    {
        provide: DialogsService,
        useClass: MetadataTemplateDialogsServiceMock,
    },
    {
        provide: SessionStorageService,
        useClass: SessionStorageServiceMock,
    },
];

const metadataTemplateManagement: Meta<MetadataTemplateManagementComponent> = {
    title: 'Sidebar/Metadata template management',
    component: MetadataTemplateManagementComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule, MdsModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat(additionalProviders),
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default metadataTemplateManagement;

type Story = StoryObj<MetadataTemplateManagementComponent>;
export const Default_view: Story = {
    args: {},
};
