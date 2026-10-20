import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBottomSheetModule } from '@angular/material/bottom-sheet';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { ApiRequestConfiguration, EduSharingApiModule } from 'ngx-edu-sharing-api';
import { translateProvider } from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { OptionItem } from '../types/option-item';
import { DropdownComponent } from './dropdown.component';

const flatOptions = [
    new OptionItem('Option 1', 'home', () => {}),
    new OptionItem('Option 2', 'help_outline', () => {}),
    new OptionItem('Option 3', 'help_outline', () => {}),
];

/** Creates a submenu option whose entries behave like a single-choice list. */
function createLanguageSubmenu(): OptionItem {
    const languages = ['Deutsch (de)', 'English (en)', 'Français (fr)', 'Español (es)'];
    const submenu = new OptionItem('Language', 'translate', () => {});
    submenu.subOptionsTitle = 'Select language';
    submenu.subOptionsSelectable = true;
    submenu.subOptions = languages.map((name, i) => {
        const option = new OptionItem(name, '', () => {
            submenu.subOptions.forEach((o, j) => (o.isSelected = i === j));
        });
        option.isSelected = i === 0;
        return option;
    });
    return submenu;
}

/** Creates a submenu option without title and without selection. */
function createPlainSubmenu(): OptionItem {
    const submenu = new OptionItem('Submenu', 'folder', () => {});
    submenu.subOptions = ['A', 'B'].map((name) => new OptionItem(name, '', () => {}));
    return submenu;
}

const dropdown: Meta<DropdownComponent> = {
    title: 'Core UI/Dropdown',
    component: DropdownComponent,
    decorators: [
        moduleMetadata({
            imports: [
                MatMenuModule,
                MatButtonModule,
                MatBottomSheetModule,
                EduSharingApiModule,
                EduSharingUiCommonModule,
            ],
        }),
        applicationConfig({
            providers: [
                provideAnimations(),
                { provide: TranslatePipe, useValue: {} },
                { provide: ApiRequestConfiguration, useValue: {} },
                { provide: TranslateService, useClass: translateProvider },
            ],
        }),
    ],
    // The dropdown only renders a hidden trigger, so an external button opens the menu.
    render: (args) => ({
        props: args,
        template: `
            <button mat-button [matMenuTriggerFor]="dd.menu">Open menu</button>
            <button mat-button (click)="dd.triggerBottomSheet()">Open bottom sheet</button>
            <es-dropdown #dd [options]="options" [position]="position"></es-dropdown>
        `,
    }),
    args: {
        options: flatOptions,
        position: 'left',
    },
    argTypes: {
        position: { control: 'select', options: ['left', 'right'] },
    },
    tags: ['autodocs'],
};

export default dropdown;
type Story = StoryObj<DropdownComponent>;

export const Flat: Story = {};

export const WithSubmenu: Story = {
    args: {
        options: [...flatOptions, createLanguageSubmenu(), createPlainSubmenu()],
    },
};

export const DisabledSubOption: Story = {
    args: {
        options: (() => {
            const submenu = createLanguageSubmenu();
            submenu.subOptions[1].isEnabled = false;
            return [...flatOptions, submenu];
        })(),
    },
};
