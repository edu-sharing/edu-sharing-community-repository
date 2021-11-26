import { Component, Input, OnInit } from '@angular/core';
import {ListWidget} from '../list-widget';
import {ListItem} from '../../../../../core-module/ui/list-item';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {NodeHelperService} from '../../../../node-helper.service';
import {Node} from '../../../../../core-module/rest/data-object';

@Component({
    selector: 'es-list-node-license',
    templateUrl: './list-node-license.component.html',
})
export class ListNodeLicenseComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', RestConstants.CCM_PROP_LICENSE),
    ]

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }

    getLicenseName() {
        return this.nodeHelper.getLicenseName(this.node as Node);
    }
    getLicenseIcon() {
        return this.nodeHelper.getLicenseIcon(this.node as Node);
    }
}
