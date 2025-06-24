import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import {
    ConfigServiceMock,
    mdsStorybookProviders,
} from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import { Toast } from '../services/abstract/toast.service';
import { ShortcutEntriesComponent } from './shortcut-entries.component';
import { Injectable } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';

@Injectable()
class ToastMock implements Toast {
    error(errorObject: any, message?: string): void {}

    toast(message: string, translationParameters?: any): void {}
}

// Additional providers necessary for selected components
const additionalProviders = [
    { provide: Toast, useClass: ToastMock }, // required for NodeEntriesService
    { provide: ConfigService, useFactory: () => new ConfigServiceMock(null, null) },
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

type Story = StoryObj<ShortcutEntriesComponent>;
export const Desktop_view: Story = {
    args: {
        fakeMobileDevice: false,
    },
};
// simulate touch device: chrome -> three points -> add device type -> desktop (touch)
export const Mobile_view: Story = {
    args: {
        fakeMobileDevice: true,
    },
};
