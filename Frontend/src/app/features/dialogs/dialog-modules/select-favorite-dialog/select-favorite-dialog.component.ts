import { Component, inject } from '@angular/core';
import {
    HOME_REPOSITORY,
    IamV1Service,
    ME,
    Node,
    RefDashboardShortcutEntry,
} from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import {
    NodesSelectorComponent,
    NodesSelectorConfig,
    TabType,
} from '../../../../pages/editorial-page/nodes-selector/nodes-selector.component';
import type { OptionState } from '../../../editorial-sidebar/editorial-sidebar.component';
import { ExtendedShortcutEntry } from '../../../shortcut-entries/shortcut-entries-types';
import { ShortcutEntriesService } from '../../../shortcut-entries/shortcut-entries.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    SelectFavoriteDialogData,
    SelectFavoriteDialogResult,
} from './select-favorite-dialog-data';

@Component({
    selector: 'es-select-favorite-dialog',
    imports: [NodesSelectorComponent],
    templateUrl: './select-favorite-dialog.component.html',
    styleUrls: ['./select-favorite-dialog.component.scss'],
})
export class SelectFavoriteDialogComponent {
    private data = inject<SelectFavoriteDialogData>(CARD_DIALOG_DATA);
    private dialogRef =
        inject<CardDialogRef<SelectFavoriteDialogData, SelectFavoriteDialogResult>>(CardDialogRef);
    private iamApi = inject(IamV1Service);
    private shortcutEntriesService = inject(ShortcutEntriesService);
    // reuse the nodes-selector, but hide the upload tab so only search, collections and
    // workspace (as in the design) are offered
    protected readonly tabBlacklist: TabType[] = [TabType.UPLOAD];

    // acts as a source picker (no parent/target) that hands the chosen node back via onNodesChoosen
    protected readonly nodesSelectorOption: OptionState<NodesSelectorConfig> = {
        option: 'SORT_INTO',
        trap: true,
        optionConfig: {
            applyLabel: 'OPTIONS.ADD_SHORTCUT',
            // exactly one node (file, folder or collection) becomes the favorite/shortcut
            applyCallback: (nodes) => nodes.length === 1,
            allowCollectionSelection: true,
            allowFolderSelection: true,
            // only the list view is needed here
            allowSearchViewSwitch: false,
            onNodesChoosen: (result) => void this.addShortcut(result.nodes?.[0]),
        },
    };

    /**
     * Adds the chosen node as a shortcut entry at the configured position and closes the dialog.
     *
     * @param selectedNode
     */
    private async addShortcut(selectedNode?: Node): Promise<void> {
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
        if (this.data.firstElement) {
            entries.unshift(entryToAdd);
        } else {
            entries.push(entryToAdd);
        }
        // save entries + close dialog
        await this.shortcutEntriesService.saveEntries(entries);
        this.dialogRef.close({ node: selectedNode });
    }
}
