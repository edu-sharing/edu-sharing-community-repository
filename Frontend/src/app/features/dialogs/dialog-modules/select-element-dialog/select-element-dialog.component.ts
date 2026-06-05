import { AfterViewInit, Component, OnDestroy, inject } from '@angular/core';
import {
    HOME_REPOSITORY,
    IamV1Service,
    ME,
    Node,
    RefDashboardShortcutEntry,
} from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { ExtendedShortcutEntry } from '../../../shortcut-entries/shortcut-entries-types';
import { ShortcutEntriesService } from '../../../shortcut-entries/shortcut-entries.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { SelectElementDialogData, SelectElementDialogResult } from './select-element-dialog-data';

@Component({
    selector: 'es-select-element-dialog',
    imports: [SharedModule],
    templateUrl: './select-element-dialog.component.html',
    styleUrls: ['./select-element-dialog.component.scss'],
})
export class SelectElementDialogComponent implements AfterViewInit, OnDestroy {
    data = inject<SelectElementDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<SelectElementDialogData, SelectElementDialogResult>>(CardDialogRef);
    private iamApi = inject(IamV1Service);
    private shortcutEntriesService = inject(ShortcutEntriesService);

    firstElement: boolean = false;

    constructor() {
        const data = this.data;

        this.firstElement = data.firstElement;
    }

    /**
     * Initializes event listener.
     */
    async ngAfterViewInit(): Promise<void> {
        this.initializeCustomEventListeners();
    }

    /**
     * On destroy, remove the event listeners.
     */
    ngOnDestroy() {
        window.removeEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the initialization of a custom event listener to react to node selections
     */
    private initializeCustomEventListeners(): void {
        window.addEventListener('message', this.handleApplyNode, false);
    }

    /**
     * Handles the receiving of the APPLY_NODE event.
     *
     * @param event
     */
    private handleApplyNode = async (event: MessageEvent<any>): Promise<void> => {
        // APPLY_NODE event was received
        if (event.data.event === 'APPLY_NODE') {
            const selectedNode: Node = event.data?.data;
            if (!selectedNode) {
                return;
            }
            // retrieve current entries
            const entries: ExtendedShortcutEntry[] = await firstValueFrom(
                this.iamApi.getDashboardShortcuts({
                    repository: HOME_REPOSITORY,
                    person: ME,
                }),
            );
            // add entry depending on the current position
            const entryToAdd: RefDashboardShortcutEntry = {
                node: selectedNode,
                type: 'ref',
            };
            if (this.firstElement) {
                entries.unshift(entryToAdd);
            } else {
                entries.push(entryToAdd);
            }
            // save entries + close dialog
            await this.shortcutEntriesService.saveEntries(entries);
            this.dialogRef.close({
                node: selectedNode,
            });
        }
    };
}
