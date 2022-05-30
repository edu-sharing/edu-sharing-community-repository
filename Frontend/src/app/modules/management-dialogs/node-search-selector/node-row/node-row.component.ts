import { Component, ContentChild, Input, TemplateRef } from '@angular/core';
import { UniversalNode } from '../../../../common/definitions';
import { ListItem } from '../../../../core-module/core.module';

@Component({
    selector: 'es-node-row',
    templateUrl: 'node-row.component.html',
    styleUrls: ['node-row.component.scss'],
})
export class NodeRowComponent {
    @ContentChild('customMetadata') customMetadataRef: TemplateRef<any>;
    @Input() node: UniversalNode;
    @Input() columns: ListItem[];

    constructor() {}
}
