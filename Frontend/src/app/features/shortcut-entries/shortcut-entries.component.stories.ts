import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { ConfigService, IamV1Service } from 'ngx-edu-sharing-api';
import { Toast } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../shared/shared.module';
import {
    ConfigServiceMock,
    IamServiceMock,
    mdsStorybookProviders,
    ToastMock,
} from '../mds/mds-editor/storybook-utils';
import { ShortcutEntriesComponent } from './shortcut-entries.component';

// Additional providers necessary for selected components
const additionalProviders = [
    { provide: Toast, useClass: ToastMock }, // required for NodeEntriesService
    { provide: ConfigService, useFactory: () => new ConfigServiceMock(null, null) },
    { provide: IamV1Service, useFactory: () => new IamServiceMock(null, null) },
];

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const shortcutEntries: Meta<ShortcutEntriesComponent> = {
    title: 'Dashboard/Shortcut entries',
    component: ShortcutEntriesComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders.concat(additionalProviders),
        }),
    ],
    args: {},
    argTypes: {},
    tags: ['autodocs'],
};

export default shortcutEntries;

// simulate a touch device: chrome -> three points -> add device type -> desktop (touch)
type Story = StoryObj<ShortcutEntriesComponent>;
export const Landing_page_view: Story = {
    args: {},
};
