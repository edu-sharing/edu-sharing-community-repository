import { CdkDragDrop, CdkDragStart, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import {
    Component,
    computed,
    ElementRef,
    HostListener,
    input,
    OnInit,
    signal,
    Signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatDivider } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import {
    ClientConfig,
    ConfigService,
    DefaultDashboardShortcutEntry,
    Node,
    RefDashboardShortcutEntry,
    ShortcutConfig,
    ShortcutConfigEntry,
} from 'ngx-edu-sharing-api';
import {
    DropdownComponent,
    EduSharingUiCommonModule,
    IconDirective,
    NodeEntriesService,
    NodeHelperService,
    OptionItem,
    UIService,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { YES_OR_NO } from '../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../dialogs/dialogs.service';
import { ExtendedShortcutEntry } from './shortcut-entries-types';
import { ShortcutEntriesService } from './shortcut-entries.service';
import { ShortcutEntryTitlePipe } from './shortcut-entry-title.pipe';

@Component({
    selector: 'es-shortcut-entries',
    providers: [IconDirective, NodeEntriesService, ShortcutEntryTitlePipe],
    templateUrl: './shortcut-entries.component.html',
    styleUrls: ['./shortcut-entries.component.scss'],
    imports: [
        CommonModule,
        DragDropModule,
        EduSharingUiCommonModule,
        FormsModule,
        MatButtonModule,
        MatCard,
        MatDivider,
        MatInputModule,
        MatMenuModule,
        ShortcutEntryTitlePipe,
        TranslateModule,
    ],
})
export class ShortcutEntriesComponent implements OnInit {
    readonly i18nPrefix: string = 'SHORTCUT_ENTRIES.';
    readonly mobileDragStartDelay: number = 1300;

    blockClickEvent: boolean = false;
    clientConfig: ClientConfig | any;
    clientConfigEntries: ShortcutConfigEntry[] = [];
    dragging: boolean;
    dropdownLeft: number;
    dropdownTop: number;
    editTitle: string;
    // entries displayed in the view
    entries: ExtendedShortcutEntry[] = [];
    entriesOverflow: WritableSignal<boolean> = signal(false);
    entryOptions: OptionItem[] = [];
    isExpanded: WritableSignal<boolean> = signal(false);
    viewExpanded: Signal<boolean> = computed(() => {
        return this.isExpanded() || this.openedInModal();
    });
    longPressTimeout: any = null;
    maxItems: number = 4;
    mobileEditMode = false;
    nodeIncluded: WritableSignal<boolean> = signal(false);
    remainingClientConfigEntries: ShortcutConfigEntry[] = [];
    renameEntryVisible: boolean = false;
    selectedEntryIndex: number = -1;

    readonly node = input<Node>();
    readonly openedInModal: Signal<boolean> = computed(() => {
        return !!this.node()?.ref.id;
    });
    @ViewChild('dropdown') dropdown: DropdownComponent;
    @ViewChild('dropdownTrigger') dropdownTrigger: MatMenuTrigger;
    @ViewChild('editInput') editInput!: ElementRef;
    @ViewChild('entriesContainer') entriesContainerRef!: ElementRef;
    @ViewChild('entriesWrapper') entriesWrapperRef!: ElementRef;

    // Note: Adding OptionsHelperDataService results in a circular dependency
    constructor(
        private configService: ConfigService,
        private dialogs: DialogsService,
        private entriesService: NodeEntriesService<Node>,
        private nodeHelper: NodeHelperService,
        private router: Router,
        private shortcutEntriesService: ShortcutEntriesService,
        private shortcutEntryTitlePipe: ShortcutEntryTitlePipe,
        private ui: UIService,
    ) {}

    /**
     * Initializes the component by defining the shortcut entry options and retrieving the client config.
     */
    async ngOnInit() {
        // define options for entries
        const optionRename = new OptionItem(
            this.i18nPrefix + 'RENAME',
            'edit',
            (node: any, nodes: any[]): void => {
                this.renameEntryVisible = true;
                // automatically focus the title edit input
                setTimeout(() => {
                    this.editInput.nativeElement.focus();
                });
            },
        );
        optionRename.enabledCallback = (nodes?: Node[]) => {
            return new Promise((resolve, reject) => {
                const result = true;
                resolve(result);
            });
        };
        const optionDelete = new OptionItem(
            this.i18nPrefix + 'DELETE',
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
            const shortcuts: ShortcutConfig = this.clientConfig.frontpage.dashboard.shortcuts;
            console.log('clientConfig shortcuts', shortcuts);
            if (shortcuts.maxEntries > 0) {
                this.maxItems = shortcuts.maxEntries;
            }
            if (shortcuts.entries?.length > 0) {
                this.clientConfigEntries = shortcuts.entries;
            }
        }
        await this.retrieveEntriesAndSync();
    }

    /**
     * On window resize, check the widths of both entry container and wrapper to decide whether an overflow exists.
     */
    @HostListener('window:resize')
    onResize() {
        this.checkOverflow();
    }

    /**
     * Adds a new shortcut entry, which might either be a shortcut config entry or a ref dashboard shortcut entry.
     *
     * @param entry
     */
    async addEntry(entry: ShortcutConfigEntry | RefDashboardShortcutEntry) {
        const isShortcutConfigEntry: boolean = 'id' in entry && !!entry.id;
        if (isShortcutConfigEntry) {
            const entryToAdd: DefaultDashboardShortcutEntry = {
                type: 'default',
            };
            if ('id' in entry) {
                entryToAdd.id = entry.id;
            }
            if (this.entriesOverflow()) {
                this.entries.unshift(entryToAdd);
            } else {
                this.entries.push(entryToAdd);
            }
            this.syncEntriesAndConfig();
            await this.shortcutEntriesService.saveEntries(this.entries);
        } else if ('type' in entry && entry.type === 'ref') {
            if (this.entriesOverflow()) {
                this.entries.unshift(entry as RefDashboardShortcutEntry);
            } else {
                this.entries.push(entry as RefDashboardShortcutEntry);
            }
            this.updateNodeIncluded();
            this.syncEntriesAndConfig();
            await this.shortcutEntriesService.saveEntries(this.entries);
        }
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
        void this.setCurrentIndexAndEditTitle(index);
    }

    /**
     * Opens the context menu by setting the current index and edit title and updating the position of the dropdown.
     * @param index
     * @param event
     */
    async openContextMenu(index: number, event: MouseEvent) {
        event.preventDefault();
        event.stopPropagation();
        await this.setCurrentIndexAndEditTitle(index);
        // update the position of the dropdown
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        }
        void this.showDropdown();
    }

    /**
     * Saves the title of the entry at the select entry index and temporarily blocks the click event.
     */
    renameEntry() {
        this.entries[this.selectedEntryIndex].title = this.editTitle;
        void this.shortcutEntriesService.saveEntries(this.entries);
        this.renameEntryVisible = false;
        this.selectedEntryIndex = -1;
        this.blockClickEvent = true;
        setTimeout(() => {
            this.blockClickEvent = false;
        }, 500);
    }

    /**
     * Deletes the entry for the selected entry index.
     */
    deleteEntry() {
        if (this.selectedEntryIndex > -1) {
            this.entries.splice(this.selectedEntryIndex, 1);
            void this.shortcutEntriesService.saveEntries(this.entries);
            this.selectedEntryIndex = -1;
            this.syncEntriesAndConfig();
            this.updateNodeIncluded();
        }
    }

    /**
     * Handles the drop of an entry by moving it inside the array.
     *
     * @param event
     */
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.entries, event.previousIndex, event.currentIndex);
        void this.shortcutEntriesService.saveEntries(this.entries);
    }

    /**
     * Handles the drag starting event by delaying it for mobile devices and resetting the dragging variable.
     *
     * @param event
     */
    onDragStart(event: CdkDragStart) {
        if (this.ui.isMobile()) {
            event.source.dragStartDelay = this.mobileDragStartDelay;
        } else {
            this.dragging = true;
        }
    }

    /**
     * Handles the drag end event by resetting the mobile edit mode, the dragging and the select entry variable.
     */
    onDragEnd() {
        this.mobileEditMode = false;
        this.dragging = false;
        this.selectedEntryIndex = -1;
    }

    /**
     * When being on mobile devices, listening for long presses to switch into the mobile edit mode.
     */
    onPressStart() {
        if (this.ui.isMobile()) {
            this.longPressTimeout = setTimeout(() => {
                this.dropdownTrigger.closeMenu();
                this.mobileEditMode = true;
                this.dragging = true;
            }, this.mobileDragStartDelay);
        }
    }

    /**
     * Clears the long press timeout if the press ended before.
     */
    onPressEnd() {
        clearTimeout(this.longPressTimeout);
        this.mobileEditMode = false;
    }

    /**
     * Handles the click event of a given entry by opening its URL in a new tab if the click is valid.
     *
     * @param entry
     * @param index
     * @param event
     */
    async entryClicked(entry: ExtendedShortcutEntry, index: number, event: MouseEvent) {
        // prevents unwanted backdrop click
        event.stopPropagation();
        // special case for modal to overwrite existing entries
        if (this.openedInModal()) {
            // if the node is already included, cancel the click event
            if (this.nodeIncluded()) {
                return;
            }
            const confirmDialogRef = await this.dialogs.openGenericDialog({
                title: this.i18nPrefix + 'REPLACE_TITLE',
                message: this.i18nPrefix + 'REPLACE_MESSAGE',
                buttons: YES_OR_NO,
            });
            confirmDialogRef.afterClosed().subscribe((response) => {
                if (response === 'YES') {
                    this.entries[index] = {
                        node: this.node(),
                        type: 'ref',
                    };
                    void this.shortcutEntriesService.saveEntries(this.entries);
                }
            });
            return;
        }
        // the click event should be canceled in the following situations:
        // * when the user is dragging
        // * when no target URL exists
        // * when the mobile edit mode is active
        // * when the renaming node input is shown
        // * when the click event is manually blocked, e.g., after saving the title
        if (
            this.dragging ||
            this.mobileEditMode ||
            this.renameEntryVisible ||
            this.blockClickEvent
        ) {
            return;
        }
        if (entry.url) {
            window.open(entry.url, '_self');
        } else if (entry.node) {
            this.router.navigate([this.nodeHelper.getNodeLink('routerLink', entry.node)], {
                queryParams: this.nodeHelper.getNodeLink('queryParams', entry.node) as any,
            });
        }
    }

    /**
     * Handles the click event on the entry add card.
     */
    async addEntryClicked() {
        if (!this.openedInModal() || this.nodeIncluded()) {
            return;
        }
        const entryToAdd: RefDashboardShortcutEntry = {
            node: this.node(),
            type: 'ref',
        };
        await this.addEntry(entryToAdd);
        this.nodeIncluded.set(true);
    }

    /**
     * Opens a modal to select an element.
     */
    async selectElement() {
        const selectElementDialogRef = await this.dialogs.openSelectElementDialog({
            firstElement: this.entriesOverflow(),
        });
        selectElementDialogRef.afterClosed().subscribe(async (response) => {
            // always reload the entries due to adding also being possible via the three-dot menu
            await this.retrieveEntriesAndSync();
        });
    }

    // HELPERS
    /**
     * Retrieves the entries, sets different values and sync both entries and config.
     */
    private async retrieveEntriesAndSync() {
        // retrieve default or user-specified entries
        this.entries = await this.shortcutEntriesService.retrieveEntries();
        // check whether the node was already added
        this.updateNodeIncluded();
        // ensure that the view is fully initialized and rendered
        setTimeout(() => {
            this.checkOverflow();
        });
        // sync both entries and client config
        this.syncEntriesAndConfig();
    }

    /**
     * Helper function to check whether the current node is already included in the view.
     */
    private updateNodeIncluded() {
        const currentNode = this.node();
        const isNodeIncluded =
            currentNode && this.entries.some((entry) => entry.node?.ref.id === currentNode.ref.id);
        this.nodeIncluded.set(isNodeIncluded);
    }

    /**
     * Helper function to check whether an overflow of both the entry container and wrapper exists.
     */
    private checkOverflow() {
        const containerWidth = this.entriesContainerRef?.nativeElement.offsetWidth;
        const wrapperWidth = this.entriesWrapperRef?.nativeElement.offsetWidth;
        this.entriesOverflow.set(containerWidth && wrapperWidth && wrapperWidth >= containerWidth);
        if (!this.entriesOverflow()) {
            this.isExpanded.set(false);
        }
    }

    /**
     * Helper function to sync both entries and config, e.g., by enriching the entries with missing icons and url or by calculating the remaining entries to add.
     */
    private syncEntriesAndConfig() {
        // enrich items with their default value being set with their icons from the config
        this.entries?.forEach((entry: ExtendedShortcutEntry) => {
            if (entry.id && !entry.icon) {
                const matchingEntry: ShortcutConfigEntry = this.clientConfigEntries.find(
                    (configEntry) => configEntry.id === entry.id,
                );
                if (matchingEntry) {
                    if (matchingEntry.icon) {
                        const prefix: string =
                            matchingEntry.icon.includes('.svg') &&
                            !matchingEntry.icon.includes('svg-')
                                ? 'svg-'
                                : '';
                        entry.icon = prefix + matchingEntry.icon;
                    }
                    if (matchingEntry.url) {
                        entry.url = matchingEntry.url;
                    }
                }
            }
        });
        // calculate the remaining client config entries that can be added
        const entryDefaults: string[] = this.entries.filter((e) => !!e.id).map((e) => e.id);
        this.remainingClientConfigEntries = this.clientConfigEntries.filter(
            (entry) => !entryDefaults.includes(entry.id),
        );
    }

    /**
     * Helper function to set the current selected index and the current edit title as shown in the view.
     *
     * @param index
     */
    private async setCurrentIndexAndEditTitle(index: number) {
        this.selectedEntryIndex = index;
        const entry: ExtendedShortcutEntry = this.entries[index];
        this.editTitle = await firstValueFrom(
            this.shortcutEntryTitlePipe.transform(entry, index, this.i18nPrefix),
        );
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
