import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { ActionbarComponent } from './actionbar.component';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OptionItem } from '../types/option-item';
import { MatMenuModule } from '@angular/material/menu';
import { translateProvider } from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { MatButtonModule } from '@angular/material/button';
import { HttpClient } from '@angular/common/http';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { ApiRequestConfiguration, EduSharingApiModule } from 'ngx-edu-sharing-api';
import { provideAnimations } from '@angular/platform-browser/animations';

let defaultOptions = [
    new OptionItem('Option 1', 'home', () => {}),
    new OptionItem('Option 2', 'help_outline', () => {}),
    new OptionItem('Option 3', 'help_outline', () => {}),
    new OptionItem('Option 4', 'help_outline', () => {}),
    new OptionItem('Option 5', 'help_outline', () => {}),
    new OptionItem('Option 6', 'help_outline', () => {}),
    new OptionItem('Option 7', 'help_outline', () => {}),
];
const actionbar: Meta<ActionbarComponent> = {
    title: 'Core UI/Actionbar',
    component: ActionbarComponent,
    decorators: [
        moduleMetadata({
            imports: [
                MatMenuModule,
                MatButtonModule,
                EduSharingApiModule,
                EduSharingUiCommonModule,
            ],
            declarations: [],
        }),
        applicationConfig({
            providers: [
                provideAnimations(),
                {
                    provide: HttpClient,
                    useValue: {},
                },
                {
                    provide: TranslatePipe,
                    useValue: {},
                },
                {
                    provide: ApiRequestConfiguration,
                    useValue: {},
                },
                {
                    provide: TranslateService,
                    useValue: translateProvider,
                },
            ],
        }),
    ],
    args: {
        backgroundType: 'primary',
        appearance: 'button',
        options: defaultOptions,
        numberOfAlwaysVisibleOptions: 2,
        numberOfAlwaysVisibleOptionsMobile: 1,
    },
    argTypes: {
        appearance: {
            control: 'button',
            options: ['button', 'round', 'icon-button'],
        },
    },
    tags: ['autodocs'],
};

export default actionbar;
type Story = StoryObj<ActionbarComponent>;
export const ActionbarPrimary: Story = {
    args: {},
};
export const ActionbarPrimaryOneOption: Story = {
    args: {
        options: [new OptionItem('Option 1', 'home', () => {})],
    },
};
export const ActionbarPrimaryTwoOptions: Story = {
    args: {
        options: [
            new OptionItem('Option 1', 'home', () => {}),
            new OptionItem('Option 2', 'help_outline', () => {}),
        ],
    },
};
export const ActionbarPrimaryOptionsDisabled: Story = {
    args: {
        options: defaultOptions.map((o) => {
            o = new OptionItem(o.name, o.icon, o.callback);
            o.isEnabled = false;
            return o;
        }),
    },
};
export const ActionbarPrimaryOptionsDisabledMenu: Story = {
    args: {
        options: defaultOptions.map((o, i) => {
            o = new OptionItem(o.name, o.icon, o.callback);
            o.isEnabled = i > 3;
            return o;
        }),
    },
};
export const ActionbarRound: Story = {
    args: {
        appearance: 'round',
    },
};
export const ActionbarOnDarkBackground: Story = {
    args: {
        backgroundType: 'dark',
    },
};
export const ActionbarOnBrightBackground: Story = {
    args: {
        backgroundType: 'bright',
    },
};
