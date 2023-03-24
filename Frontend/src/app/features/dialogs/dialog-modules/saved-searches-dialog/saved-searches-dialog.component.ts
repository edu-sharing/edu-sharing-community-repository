import { Component, Inject, Injector, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NodeService, SavedSearchesService, Node } from 'ngx-edu-sharing-api';
import { map, take } from 'rxjs/operators';
import {
    DialogButton,
    ListItem,
    RestConstants,
    UIConstants,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../core-ui-module/node-helper.service';
import { Scope } from '../../../../core-ui-module/option-item';
import { InteractionType, NodeEntriesDisplayType } from '../../../node-entries/entries-model';
import { NodeDataSourceRemote } from '../../../node-entries/node-data-source-remote';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { SavedSearchesDialogData, SavedSearchesDialogResult } from './saved-searches-dialog-data';

@Component({
    selector: 'es-saved-searches-dialog',
    templateUrl: './saved-searches-dialog.component.html',
    styleUrls: ['./saved-searches-dialog.component.scss'],
})
export class SavedSearchesDialogComponent implements OnInit {
    @ViewChild('saveCurrentSearch', { static: true })
    saveCurrentSearchRef: TemplateRef<HTMLElement>;

    readonly mySavedSearchesSource = new NodeDataSourceRemote(this.injector);
    readonly columns = [new ListItem('NODE', 'title')];
    readonly displayType = NodeEntriesDisplayType.Table;
    readonly scope = Scope.SavedSearches;
    readonly interactionType = InteractionType.Emitter;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: SavedSearchesDialogData,
        private dialogRef: CardDialogRef<SavedSearchesDialogData, SavedSearchesDialogResult>,
        private dialogs: DialogsService,
        private savedSearchesService: SavedSearchesService,
        private nodeHelper: NodeHelperService,
        private router: Router,
        private nodeService: NodeService,
        private injector: Injector,
    ) {
        this.mySavedSearchesSource.setRemote(() =>
            this.savedSearchesService.observeSavedSearches().pipe(
                map((savedSearches) => ({
                    data: savedSearches.map(({ node }) => node),
                    total: savedSearches.length,
                })),
            ),
        );
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            customHeaderBarContent: this.saveCurrentSearchRef,
            buttons: [
                new DialogButton(
                    'SEARCH.SAVED_SEARCHES.MANAGE_SAVED_SEARCH',
                    { color: 'standard' },
                    () => this.manageSavedSearches(),
                ),
            ],
        });
    }

    selectSavedSearch(node: Node): void {
        this.savedSearchesService
            .observeSavedSearches()
            .pipe(take(1))
            .subscribe((savedSearches) => {
                const savedSearch = savedSearches.find((savedSearch) => savedSearch.node === node);
                this.dialogRef.close(savedSearch);
            });
    }

    async openSaveSearchDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openSaveSearchDialog(this.data.saveSearchData);
        // const savedSearch = await dialogRef.afterClosed().toPromise();
        // this.currentSavedSearch = savedSearch;
        // return savedSearch;
    }

    manageSavedSearches(): void {
        this.dialogRef.close();
        this.nodeService.getNode(RestConstants.SAVED_SEARCH).subscribe((node) =>
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'workspace/files'], {
                queryParams: { id: node.ref.id },
            }),
        );
    }

    // async embedCurrentSearch(): Promise<void> {
    //     let savedSearch = this.currentSavedSearch;
    //     if (!savedSearch) {
    //         savedSearch = await this.openSaveSearchDialog();
    //     }
    //     if (savedSearch) {
    //         this.nodeHelper.addNodeToLms(
    //             this.currentSavedSearch.node,
    //             this.searchPage.reUrl.value as string,
    //         );
    //     }
    // }
}
