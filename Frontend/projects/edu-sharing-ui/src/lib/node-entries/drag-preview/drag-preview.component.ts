import { Component, Input } from '@angular/core';
import { ListItem } from '../../types/list-item';
import { Node } from 'ngx-edu-sharing-api';
import { NodeEntriesDataType } from '../entries-model';

@Component({
    selector: 'es-drag-preview',
    templateUrl: './drag-preview.component.html',
    styleUrls: ['./drag-preview.component.scss'],
})
export class DragPreviewComponent<T extends NodeEntriesDataType> {
    @Input() node: Node;
    @Input() selected: T[];
    @Input() item: ListItem;
}
