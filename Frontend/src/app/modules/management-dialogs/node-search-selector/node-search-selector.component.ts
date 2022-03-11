import {AfterViewInit, Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {FormControl} from '@angular/forms';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {combineAll, debounceTime, map, startWith, switchMap} from 'rxjs/operators';
import {RestSearchService} from '../../../core-module/rest/services/rest-search.service';
import {Node, SearchRequestCriteria} from '../../../core-module/rest/data-object';
import {RestConstants} from '../../../core-module/rest/rest-constants';
import {
    MdsEditorWrapperComponent
} from '../../../common/ui/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import {Values} from '../../../common/ui/mds-editor/types';
import {MdsHelper} from '../../../core-module/rest/mds-helper';
import {RestMdsService} from '../../../core-module/rest/services/rest-mds.service';
import {TranslateService} from '@ngx-translate/core';
import {ListItem} from '../../../core-module/ui/list-item';
import * as rxjs from 'rxjs';



@Component({
    selector: 'es-node-search-selector',
    templateUrl: 'node-search-selector.component.html',
    styleUrls: ['node-search-selector.component.scss'],
    animations: [
    ]
})
export class NodeSearchSelectorComponent implements AfterViewInit{
    @ViewChild(MdsEditorWrapperComponent) mdsEditor: MdsEditorWrapperComponent;
    /**
     * group id of the mds set to use the search filters from
     */
    @Input() groupId: string;
    /**
     * query id to use for the search query
     */
    @Input() queryId: string;
    /**
     * the id to use for the displayed columns
     */
    @Input() columnsIds: string;
    /**
     * label of the search field
     */
    @Input() label: string;
    /**
     * only show nodes with appropriate permissions
     */
    @Input() permissions: string[] = [];
    /**
     * count of items to search
     */
    @Input() itemCount = 100;
    @Output() onSelect = new EventEmitter<Node>();
    searchResult$: Observable<Node[]>;
    input = new FormControl('');
    columns: ListItem[];
    showMds = false;
    private values: { [p: string]: string[] };

    constructor(
        private searchApi: RestSearchService,
        private mdsService: RestMdsService,
        private translate: TranslateService,
    ) {
    }

    setOption(event: MatAutocompleteSelectedEvent) {
        this.onSelect.emit(event.option.value);
        this.input.setValue('');
    }

    private searchNodes(): Observable<Node[]> {
        let criterias: SearchRequestCriteria[] = [];
        if (this.input.value) {
            criterias.push({
                property: RestConstants.PRIMARY_SEARCH_CRITERIA,
                values: [this.input.value],
            });
        }
        if (this.values) {
            criterias = criterias.concat(
                RestSearchService.convertCritierias(
                    this.values,
                    this.mdsEditor.currentWidgets,
                ),
            );
        }
        const request = {
            maxItems: this.itemCount,
        };
        return this.searchApi.search(
            criterias,
            [],
            request,
            RestConstants.CONTENT_TYPE_ALL,
            RestConstants.HOME_REPOSITORY,
            RestConstants.DEFAULT,
            [RestConstants.ALL],
            this.queryId,
            this.permissions
        ).pipe(
            map(m => m.nodes)
        );
    }

    ngAfterViewInit(): void {
        this.mdsService.getSet().subscribe((set) => {
            this.columns = MdsHelper.getColumns(this.translate, set,this.columnsIds);
        });
        this.searchResult$ = combineLatest([
            this.input.valueChanges,
        ]).pipe(
            debounceTime(200),
            switchMap(() => this.searchNodes()),
        );
        this.mdsEditor.mdsEditorInstance.values.subscribe((v) => this.values = v);
    }
}
