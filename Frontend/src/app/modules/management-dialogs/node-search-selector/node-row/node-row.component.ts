
import {Component, ContentChild, EventEmitter, Input, Output, TemplateRef} from '@angular/core';
import {Group, Node, User} from '../../../../core-module/rest/data-object';
import {
    ListItem,
    RestIamService,
    RestMdsService,
    RestOrganizationService
} from '../../../../core-module/core.module';
import {PermissionNamePipe} from '../../../../core-ui-module/pipes/permission-name.pipe';
import {MdsHelper} from '../../../../core-module/rest/mds-helper';
import {UniversalNode} from '../../../../common/definitions';

@Component({
    selector: 'es-node-row',
    templateUrl: 'node-row.component.html',
    styleUrls: ['node-row.component.scss'],
})
export class NodeRowComponent {
    @ContentChild('customMetadata') customMetadataRef: TemplateRef<any>;
    @Input() node: UniversalNode;
    @Input() columns: ListItem[];

    constructor(
    ) {
    }

}
