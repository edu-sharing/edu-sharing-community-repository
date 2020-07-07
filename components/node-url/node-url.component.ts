import {Component, Input} from '@angular/core';
import {Node} from '../../../core-module/rest/data-object';
import {Params} from "@angular/router";
import {NodeHelper} from "../../node-helper";
import {UIConstants} from "../../../core-module/ui/ui-constants";
import {RestNetworkService} from "../../../core-module/rest/services/rest-network.service";

@Component({
  selector: 'app-node-url',
  template: `
    <ng-template #content><ng-content></ng-content></ng-template>
    <a *ngIf="unclickable">
      <ng-container *ngTemplateOutlet="content"></ng-container>
    </a>
    <a *ngIf="!unclickable"
      [routerLink]="get('routerLink')"
      [state]="getState()"
      [queryParams]="get('queryParams')">
      <ng-container *ngTemplateOutlet="content"></ng-container>
    </a>`,
})
export class NodeUrlComponent {
  private _node: Node;
  @Input() set node (node: Node) {
    this._node = node;
  }
  @Input() nodes: Node[];
  @Input() scope: string;
  @Input() unclickable: boolean;
  constructor() {
  }
  getState() {
    return {
      nodes: this.nodes,
      scope: this.scope
    };
  }
  get(mode: 'routerLink' | 'queryParams'): any {
    if (!this._node) {
      return null;
    }
    let data: { routerLink: string, queryParams: Params } = null;
    if (NodeHelper.isNodeCollection(this._node)) {
      data = {
        routerLink: UIConstants.ROUTER_PREFIX + 'collections',
        queryParams: { id: this._node.ref.id },
      };
    } else {
      if (this._node.isDirectory) {
        data = {
          routerLink: UIConstants.ROUTER_PREFIX + 'workspace',
          queryParams: { id: this._node.ref.id },
        };
      } else if(this._node.ref) {
        const fromeHome = RestNetworkService.isFromHomeRepo(this._node);
        data = {
          routerLink: UIConstants.ROUTER_PREFIX + 'render/' + this._node.ref.id,
          queryParams: {
            repository: fromeHome ? null : this._node.ref.repo,
          },
        };
      }
    }
    if(data === null){
      return '';
    }
    if(mode === 'routerLink') {
      return '/' + data.routerLink;
    }
    return data.queryParams;
  }
}
