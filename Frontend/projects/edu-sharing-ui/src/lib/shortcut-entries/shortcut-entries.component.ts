import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ClientConfig, ConfigService, Node } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { map } from 'rxjs/operators';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { IconDirective } from '../directives/icon.directive';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { NodeTitlePipe } from '../pipes/node-title.pipe';
import { NodeEntriesService } from '../services/node-entries.service';
import { UIService } from '../services/ui.service';
import { OptionItem } from '../types/option-item';

export interface ShortcutEntry {
    type: 'ref' | 'default';
    default?: string;
    icon?: string;
    title?: string;
    url?: string;
    node?: Node;
    updates?: number;
}

@Component({
    selector: 'es-shortcut-entries',
    providers: [IconDirective, NodeEntriesService, NodeTitlePipe],
    templateUrl: './shortcut-entries.component.html',
    styleUrls: ['./shortcut-entries.component.scss'],
    imports: [
        CommonModule,
        DragDropModule,
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatCard,
        MatInputModule,
        MatMenuModule,
        TranslateModule,
    ],
})
export class ShortcutEntriesComponent implements OnInit {
    readonly i18nPrefix: string = 'TILE_ITEMS.';

    blockClickEvent: boolean = false;
    clientConfig: ClientConfig | any;
    clientConfigEntries: any[] = [];
    dragging: boolean;
    dropdownLeft: number;
    dropdownTop: number;
    editTitle: string;
    // entries to show (default is later replaced by API load)
    entries: ShortcutEntry[] = [
        {
            type: 'default',
            default: 'search',
            title: null,
            node: null,
        },
        {
            type: 'default',
            default: 'workspace',
            title: null,
            node: null,
        },
        {
            type: 'default',
            default: 'myfiles',
            title: null,
            node: null,
        },
        {
            type: 'default',
            default: 'mycollections',
            title: null,
            node: null,
        },
        {
            type: 'default',
            default: 'invitedcollections',
            title: null,
            node: null,
        },
    ];
    entryOptions: OptionItem[] = [];
    longPressTimeout: any = null;
    maxItems: number = 4;
    mobileEditMode = false;
    remainingClientConfigEntries: any[] = [];
    renameEntryVisible: boolean = false;
    selectedEntryIndex: number = -1;

    @Input() fakeMobileDevice: boolean = false;
    @ViewChild('dropdown') dropdown: DropdownComponent;
    @ViewChild('dropdownTrigger') dropdownTrigger: MatMenuTrigger;

    // Note: Adding OptionsHelperDataService results in circular dependency
    constructor(
        private configService: ConfigService,
        private entriesService: NodeEntriesService<Node>,
        private nodeTitlePipe: NodeTitlePipe,
        private translate: TranslateService,
        private ui: UIService,
    ) {}

    /**
     * Initializes the component by defining the shortcut entry options and retrieving the client config.
     */
    async ngOnInit() {
        // define options for entries
        const optionRename = new OptionItem('RENAME', 'edit', (node: any, nodes: any[]): void => {
            this.renameEntryVisible = true;
        });
        optionRename.enabledCallback = (nodes?: Node[]) => {
            return new Promise((resolve, reject) => {
                const result = true;
                resolve(result);
            });
        };
        const optionDelete = new OptionItem(
            'OPTIONS.DELETE',
            'delete',
            (node: any, nodes: any[]): void => {
                this.deleteEntry();
            },
        );
        optionDelete.enabledCallback = (nodes?: Node[]) => {
            return new Promise((resolve, reject) => {
                const result = true;
                resolve(result);
            });
        };
        this.entryOptions.push(optionRename, optionDelete);
        // retrieve client config
        this.clientConfig = await firstValueFrom(this.configService.observeConfig());
        console.log('clientConfig', this.clientConfig);
        if (this.clientConfig.frontpage?.dashboard?.shortcuts) {
            const shortcuts = this.clientConfig.frontpage.dashboard.shortcuts;
            console.log('clientConfig shortcuts', shortcuts);
            if (shortcuts.maxEntries > 0) {
                this.maxItems = shortcuts.maxEntries;
            }
            if (shortcuts.entries?.length > 0) {
                this.clientConfigEntries = shortcuts.entries;
            }
        }
        // sync both entries and client config
        this.syncEntriesAndConfig();
    }

    /**
     * Adds a new shortcut entry by copying its ID as default, pushing it and syncing both entries and config.
     *
     * @param entry
     */
    addEntry(entry: any) {
        const entryToAdd: ShortcutEntry = {
            type: 'default',
            default: entry.id,
            title: null,
            node: null,
        };
        this.entries.push(entryToAdd);
        this.syncEntriesAndConfig();
    }

    /**
     * Reacts to the click event of the menu button to set the selected entry index and the current edit title.
     *
     * @param index
     * @param event
     */
    setSelectedMenuIndex(index: number, event: MouseEvent) {
        event.preventDefault();
        // prevents unwanted close
        event.stopPropagation();
        this.setCurrentIndexAndEditTitle(index);
    }

    /**
     * Opens the context menu by setting the current index and edit title and updating the position of the dropdown.
     * @param index
     * @param event
     */
    async openContextMenu(index: number, event: MouseEvent) {
        event.preventDefault();
        event.stopPropagation();
        this.setCurrentIndexAndEditTitle(index);
        // update the position of the dropdown
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        }
        void this.showDropdown();
    }

    /**
     * Saves the title of the entry at the select entry index and temporary blocks the click event.
     */
    renameEntry() {
        this.entries[this.selectedEntryIndex].title = this.editTitle;
        this.renameEntryVisible = false;
        this.selectedEntryIndex = -1;
        this.blockClickEvent = true;
        setTimeout(() => {
            console.log('timeout');
            this.blockClickEvent = false;
        }, 500);
    }

    /**
     * Deletes the entry for the selected entry index.
     */
    deleteEntry() {
        if (this.selectedEntryIndex > -1) {
            this.entries.splice(this.selectedEntryIndex, 1);
            this.selectedEntryIndex = -1;
            this.syncEntriesAndConfig();
        }
    }

    /**
     * Checks, whether drag-and-drop is enabled by listening to touch events.
     */
    getDragEnabled() {
        return this.ui.isTouchSubject.pipe(map((touch: boolean) => !touch));
    }

    /**
     * Handles the drop of an entry by moving it inside the array.
     *
     * @param event
     */
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.entries, event.previousIndex, event.currentIndex);
    }

    /**
     * When being on mobile devices, listening for long presses to switch into the mobile edit mode.
     */
    onPressStart() {
        if (this.ui.isMobile() || this.fakeMobileDevice) {
            this.longPressTimeout = setTimeout(() => {
                this.mobileEditMode = true;
            }, 600); // 600ms long-press threshold
        }
    }

    /**
     * Clears the long press timeout, if the press ended before.
     */
    onPressEnd() {
        clearTimeout(this.longPressTimeout);
    }

    /**
     * Handles the click event of a given entry by opening its URL in a new tab, if the click is valid.
     *
     * @param item
     * @param event
     */
    entryClicked(item: ShortcutEntry, event: MouseEvent) {
        // prevents unwanted backdrop click
        event.stopPropagation();
        // the click event should be canceled in the following situations:
        // * when the user is dragging
        // * when no target URL exists
        // * when the mobile edit mode is active
        // * when the renaming node input is shown
        // * when the click event is manually blocked, e.g., after saving the title
        if (
            this.dragging ||
            !item.url ||
            this.mobileEditMode ||
            this.renameEntryVisible ||
            this.blockClickEvent
        ) {
            return;
        }
        window.open(item.url, '_blank');
    }

    // HELPERS
    /**
     * Helper function to sync both items and config, e.g., by enriching the items with missing icons and url or by calculating the remaining entries to add.
     */
    private syncEntriesAndConfig() {
        // enrich items with default value being set with their icons from the config
        this.entries?.forEach((item: ShortcutEntry) => {
            if (item.default && !item.icon) {
                const matchingItem = this.clientConfigEntries.find(
                    (entry) => entry.id === item.default,
                );
                if (matchingItem) {
                    const prefix: string =
                        matchingItem.icon.includes('.svg') && !matchingItem.icon.includes('svg-')
                            ? 'svg-'
                            : '';
                    item.icon = prefix + matchingItem.icon;
                    if (matchingItem.url) {
                        item.url = matchingItem.url;
                    }
                }
            }
        });
        // calculate the remaining client config entries that can be added
        const itemDefaults = this.entries.map((item: ShortcutEntry) => item.default);
        this.remainingClientConfigEntries = this.clientConfigEntries.filter(
            (entry) => !itemDefaults.includes(entry.id),
        );
    }

    /**
     * Helper function to set the current selected index and the current edit title as shown in the view.
     *
     * @param index
     */
    private setCurrentIndexAndEditTitle(index: number) {
        this.selectedEntryIndex = index;
        const item: ShortcutEntry = this.entries[index];
        // perform the same title transformation as it is done in the view
        // this might be moved to a pipe
        this.editTitle =
            item.title ||
            this.nodeTitlePipe.transform(item.node) ||
            this.translate.instant(this.i18nPrefix + item.default) ||
            'Unnamed ' + (index + 1);
    }

    /**
     * Helper function to open the dropdown menu.
     */
    private async showDropdown() {
        this.entriesService.openDropdown(this.dropdown, null, () =>
            this.dropdownTrigger.openMenu(),
        );
    }
}
