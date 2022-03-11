import { Component, EventEmitter, Input, Output } from '@angular/core';
import {Group, Node, User} from '../../../../core-module/rest/data-object';
import {
    ListItem,
    RestIamService,
    RestMdsService,
    RestOrganizationService
} from '../../../../core-module/core.module';
import {PermissionNamePipe} from '../../../../core-ui-module/pipes/permission-name.pipe';
import {MdsHelper} from '../../../../core-module/rest/mds-helper';

@Component({
    selector: 'es-node-row',
    templateUrl: 'node-row.component.html',
    styleUrls: ['node-row.component.scss'],
})
export class NodeRowComponent {
    @Input() node: Node;
    @Input() columns: ListItem[];

    constructor(
    ) {
    }

}
