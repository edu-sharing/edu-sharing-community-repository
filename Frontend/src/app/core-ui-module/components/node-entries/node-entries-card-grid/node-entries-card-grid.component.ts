import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../../options-helper.service';
import {
    AfterViewInit,
    Component,
    ContentChild, Input,
    OnChanges,
    SimpleChanges,
    TemplateRef
} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {SortEvent} from '../../sort-dropdown/sort-dropdown.component';
import {$e} from 'codelyzer/angular/styles/chars';
import {CdkDragDrop, CdkDragEnter, moveItemInArray} from '@angular/cdk/drag-drop';
import {NodeEntriesDisplayType} from '../../node-entries-wrapper/node-entries-wrapper.component';

@Component({
    selector: 'app-node-entries-card-grid',
    templateUrl: 'node-entries-card-grid.component.html',
    styleUrls: ['node-entries-card-grid.component.scss'],
})
export class NodeEntriesCardGridComponent<T extends Node> implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    @Input() displayType: NodeEntriesDisplayType;

    constructor(
        public entriesService: NodeEntriesService<T>,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
    }

    changeSort(sort: SortEvent) {
        this.entriesService.sort.active = sort.name;
        this.entriesService.sort.direction = sort.ascending ? 'asc' : 'desc';
        this.entriesService.sortChange.emit(this.entriesService.sort);
    }

    reorder(drag: CdkDragDrop<number>) {
        moveItemInArray(this.entriesService.dataSource.getData(), drag.previousContainer.data, drag.container.data);

    }
}
