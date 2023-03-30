import {
    Component,
    Inject,
    Injector,
    OnDestroy,
    OnInit,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { Node, SavedSearchesService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Subject } from 'rxjs';
import { filter, first, map, startWith, switchMap, take, takeUntil } from 'rxjs/operators';
import { DialogButton, ListItem } from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../core-ui-module/node-helper.service';
import { Scope } from '../../../../core-ui-module/option-item';
import { notNull } from '../../../../util/functions';
import { InteractionType, NodeEntriesDisplayType } from '../../../node-entries/entries-model';
import { NodeDataSourceRemote, NodeResponse } from '../../../node-entries/node-data-source-remote';
import { NodeEntriesWrapperComponent } from '../../../node-entries/node-entries-wrapper.component';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { DialogsService } from '../../dialogs.service';
import { SavedSearchesDialogData, SavedSearchesDialogResult } from './saved-searches-dialog-data';

@Component({
    selector: 'es-saved-searches-dialog',
    templateUrl: './saved-searches-dialog.component.html',
    styleUrls: ['./saved-searches-dialog.component.scss'],
})
export class SavedSearchesDialogComponent implements OnInit, OnDestroy {
    /** Template that includes the "Save current search" button. */
    @ViewChild('saveCurrentSearch', { static: true })
    saveCurrentSearchRef: TemplateRef<HTMLElement>;
    private _nodeEntries = new BehaviorSubject<NodeEntriesWrapperComponent<Node>>(null);
    /**
     * The current node-entries component.
     *
     * Updates when tabs are switched.
     */
    @ViewChild(NodeEntriesWrapperComponent)
    get nodeEntries(): NodeEntriesWrapperComponent<Node> {
        return this._nodeEntries.value;
    }
    set nodeEntries(value: NodeEntriesWrapperComponent<Node>) {
        this._nodeEntries.next(value);
    }

    readonly mySavedSearchesSource = new NodeDataSourceRemote(this.injector);
    readonly columns = [new ListItem('NODE', 'title')];
    readonly displayType = NodeEntriesDisplayType.Table;
    readonly scope = Scope.SavedSearches;
    readonly interactionType = InteractionType.Emitter;

    private destroyed = new Subject<void>();

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: SavedSearchesDialogData,
        private dialogRef: CardDialogRef<SavedSearchesDialogData, SavedSearchesDialogResult>,
        private dialogs: DialogsService,
        private savedSearchesService: SavedSearchesService,
        private nodeHelper: NodeHelperService,
        private injector: Injector,
    ) {
        this.registerMySavedSearchesSource();
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            customHeaderBarContent: this.saveCurrentSearchRef,
            buttons: this.getButtons(),
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerMySavedSearchesSource(): void {
        const subject = new BehaviorSubject<NodeResponse<Node>>(null);
        this.savedSearchesService
            .observeSavedSearches()
            .pipe(
                takeUntil(this.destroyed),
                map((savedSearches) => ({
                    data: savedSearches.map(({ node }) => node),
                    total: savedSearches.length,
                })),
            )
            .subscribe(subject);
        // `NodeDataSourceRemote` doesn't account for remote observables that emit multiple times.
        // So we set a new remote each time `observeSavedSearches` emits.
        subject.subscribe(() =>
            this.mySavedSearchesSource.setRemote(() => subject.pipe(first(notNull))),
        );
    }

    private getButtons(): DialogButton[] {
        const buttons: DialogButton[] = [];
        if (this.data.reUrl) {
            buttons.push(
                new DialogButton('SEARCH.SAVED_SEARCHES.EMBED_BUTTON', { color: 'standard' }, () =>
                    this.embedSelectedSearch(),
                ),
            );
        }
        buttons.push(
            new DialogButton('SEARCH.SAVED_SEARCHES.SEARCH_BUTTON', { color: 'primary' }, () =>
                this.returnSavedSearch(),
            ),
        );
        this._nodeEntries
            .pipe(
                filter(notNull),
                switchMap((nodeEntries) => nodeEntries.getSelection().changed),
                startWith(void 0),
            )
            .subscribe(() => {
                const buttonsEnabled = this.nodeEntries?.getSelection().selected.length === 1;
                buttons.forEach((button) => (button.disabled = !buttonsEnabled));
            });
        return buttons;
    }

    onClick(node: Node): void {
        if (this.data.reUrl) {
            this.nodeEntries.getSelection().clear();
            this.nodeEntries.getSelection().select(node);
        } else {
            this.returnSavedSearch(node);
        }
    }

    returnSavedSearch(node: Node = this.nodeEntries.getSelection().selected[0]): void {
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
        dialogRef.afterClosed().subscribe((savedSearch) => {
            this.nodeEntries.getSelection().clear();
            this.nodeEntries.getSelection().select(savedSearch.node);
        });
    }

    async embedSelectedSearch(): Promise<void> {
        let savedSearch = this.nodeEntries.getSelection().selected[0];
        this.nodeHelper.addNodeToLms(savedSearch, this.data.reUrl);
    }
}
