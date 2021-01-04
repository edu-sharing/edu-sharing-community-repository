import { Component, Input } from '@angular/core';
import { Params } from '@angular/router';
import { Node } from '../../../core-module/rest/data-object';
import { RestNetworkService } from '../../../core-module/rest/services/rest-network.service';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import {NodeHelperService} from '../../node-helper.service';

@Component({
    selector: 'app-node-url',
    template: `
        <ng-template #content><ng-content></ng-content></ng-template>
        <a *ngIf="unclickable">
            <ng-container *ngTemplateOutlet="content"></ng-container>
        </a>
        <a
            *ngIf="!unclickable"
            [routerLink]="get('routerLink')"
            [state]="getState()"
            [queryParams]="get('queryParams')"
            [attr.aria-label]="node.name"
        >
            <ng-container *ngTemplateOutlet="content"></ng-container>
        </a>
    `,
    styleUrls: ['node-url.component.scss'],
})
export class NodeUrlComponent {
    @Input() node: Node;
    @Input() nodes: Node[];
    @Input() scope: string;
    @Input() unclickable: boolean;

    constructor(private nodeHelper: NodeHelperService) {}

    getState() {
        return {
            nodes: this.nodes,
            scope: this.scope,
        };
    }

    get(mode: 'routerLink' | 'queryParams'): any {
        return this.nodeHelper.getNodeLink(mode, this.node);
    }
}
