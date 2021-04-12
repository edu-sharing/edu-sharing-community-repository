import { Component, Input } from '@angular/core';
import { Params } from '@angular/router';
import { Node } from '../../../core-module/rest/data-object';
import { RestNetworkService } from '../../../core-module/rest/services/rest-network.service';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import {NodeHelperService} from '../../node-helper.service';
import {ListTableComponent} from '../list-table/list-table.component';

@Component({
    selector: 'app-node-url',
    template: `
        <ng-template #content><ng-content></ng-content></ng-template>
        <a *ngIf="unclickable"
           matRipple matRippleColor="primary"
        >
            <ng-container *ngTemplateOutlet="content"></ng-container>
        </a>
        <a
            *ngIf="!unclickable"
            matRipple matRippleColor="primary"
            [routerLink]="get('routerLink')"
            [state]="getState()"
            [queryParams]="get('queryParams')"
            queryParamsHandling="merge"
            [attr.aria-label]="listTable?.getPrimaryTitle(node) || node.name"
            [attr.aria-describedby]="ariaDescribedby"
>
            <ng-container *ngTemplateOutlet="content"></ng-container>
        </a>
    `,
    styleUrls: ['node-url.component.scss'],
})
export class NodeUrlComponent {
    @Input() listTable: ListTableComponent;
    @Input() node: Node;
    @Input() nodes: Node[];
    @Input() scope: string;
    @Input() unclickable: boolean;
    @Input('aria-describedby') ariaDescribedby: string;

    constructor(private nodeHelper: NodeHelperService) {}

    getState() {
        return {
            scope: this.scope,
        };
    }

    get(mode: 'routerLink' | 'queryParams'): any {
        return this.nodeHelper.getNodeLink(mode, this.node);
    }
}
