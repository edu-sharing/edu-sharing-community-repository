import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import {
    I18nModule,
    mdsStorybookProviders,
} from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { SharedModule } from '../../../../../src/app/shared/shared.module';
import { Toast } from '../services/abstract/toast.service';
import { ShortcutEntriesComponent } from './shortcut-entries.component';

// Workaround to get the translations running
const staticTranslateLoader: TranslateLoader = {
    getTranslation(lang: string): Observable<any> {
        return of(require('src/assets/i18n/common/de.json'));
    },
};

// Additional providers necessary for selected components
const additionalProviders = [
    Toast, // required for NodeEntriesService
];

// More on how to set up stories at: https://storybook.js.org/docs/angular/writing-stories/introduction
const shortcutEntries: Meta<ShortcutEntriesComponent> = {
    title: 'Dashboard/Shortcut entries',
    component: ShortcutEntriesComponent,
    decorators: [
        moduleMetadata({
            imports: [
                I18nModule,
                TranslateModule.forRoot({
                    loader: {
                        provide: TranslateLoader,
                        useValue: staticTranslateLoader,
                    },
                }),
                SharedModule,
            ],
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
