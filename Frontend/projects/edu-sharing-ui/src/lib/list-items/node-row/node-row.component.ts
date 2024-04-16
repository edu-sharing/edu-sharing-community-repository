import { Component, ContentChild, Input, TemplateRef } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ListItem } from '../../types/list-item';

@Component({
    selector: 'es-node-row',
    templateUrl: 'node-row.component.html',
    styleUrls: ['node-row.component.scss'],
})
export class NodeRowComponent {
    @ContentChild('customMetadata') customMetadataRef: TemplateRef<any>;
    @Input() node: Node;
    @Input() columns: ListItem[];

    constructor() {}
}
