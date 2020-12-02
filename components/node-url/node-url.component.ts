import { Component, Input } from '@angular/core';
import { Params } from '@angular/router';
import { Node } from '../../../core-module/rest/data-object';
import { RestNetworkService } from '../../../core-module/rest/services/rest-network.service';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { NodeHelper } from '../../node-helper';

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
        >
            <ng-container *ngTemplateOutlet="content"></ng-container>
        </a>
    `,
    styleUrls: ['node-url.component.scss'],
})
export class NodeUrlComponent {
    @Input() set node(node: Node) {
        this._node = node;
    }
    @Input() nodes: Node[];
    @Input() scope: string;
    @Input() unclickable: boolean;

    private _node: Node;

    static getInfo(mode: 'routerLink' | 'queryParams', node: Node) {
        if (!node) {
            return null;
        }
        let data: { routerLink: string; queryParams: Params } = null;
        if (NodeHelper.isNodeCollection(node)) {
            data = {
                routerLink: UIConstants.ROUTER_PREFIX + 'collections',
                queryParams: { id: node.ref.id },
            };
        } else {
            if (node.isDirectory) {
                data = {
                    routerLink: UIConstants.ROUTER_PREFIX + 'workspace',
                    queryParams: { id: node.ref.id },
                };
            } else if (node.ref) {
                const fromHome = RestNetworkService.isFromHomeRepo(node);
                data = {
                    routerLink: UIConstants.ROUTER_PREFIX + 'render/' + node.ref.id,
                    queryParams: {
                        repository: fromHome ? null : node.ref.repo,
                    },
                };
            }
        }
        if (data === null) {
            return '';
        }
        if (mode === 'routerLink') {
            return '/' + data.routerLink;
        }
        return data.queryParams;
    }

    constructor() {}

    getState() {
        return {
            nodes: this.nodes,
            scope: this.scope,
        };
    }

    get(mode: 'routerLink' | 'queryParams'): any {
        return NodeUrlComponent.getInfo(mode, this._node);
    }
}
