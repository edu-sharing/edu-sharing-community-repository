import {Component, Input} from '@angular/core';
import {Node} from '../../../core-module/rest/data-object';
import {Params} from "@angular/router";
import {NodeHelper} from "../../node-helper";
import {UIConstants} from "../../../core-module/ui/ui-constants";
import {RestNetworkService} from "../../../core-module/rest/services/rest-network.service";
import {RestConstants} from '../../../core-module/rest/rest-constants';

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
      [queryParams]="get('queryParams')"
      queryParamsHandling="merge"
    >
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
    return NodeUrlComponent.getInfo(mode, this._node);
  }
  static getInfo(mode: 'routerLink' | 'queryParams', node: Node){
    if (!node) {
      return null;
    }
    let data: { routerLink: string, queryParams: Params } = null;
    if (NodeHelper.isNodeCollection(node)) {
      data = {
        routerLink: UIConstants.ROUTER_PREFIX + 'collections',
        queryParams: { id: node.ref.id },
      };
    } else {
      if (node.isDirectory) {
        let path;
        if (node.properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.[0] === RestConstants.SAFE_SCOPE) {
          path = UIConstants.ROUTER_PREFIX + 'workspace/safe';
        } else {
          path = UIConstants.ROUTER_PREFIX + 'workspace';
        }
        data = {
          routerLink: path,
          queryParams: { id: node.ref.id },
        };
      } else if(node.ref) {
        const fromeHome = RestNetworkService.isFromHomeRepo(node);
        data = {
          routerLink: UIConstants.ROUTER_PREFIX + 'render/' + node.ref.id,
          queryParams: {
            repository: fromeHome ? null : node.ref.repo,
          },
        };
      }
    }
    if(data === null) {
      return '';
    }
    if(mode === 'routerLink') {
      return '/' + data.routerLink;
    }
    return data.queryParams;
  }
}
