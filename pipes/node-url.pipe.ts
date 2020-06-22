import {PipeTransform, Pipe} from '@angular/core';
import {Translation} from "../translation";
import {Router} from "@angular/router";
import {Node} from "../../core-module/rest/data-object";
import {NodeHelper} from "../node-helper";
import {UIConstants} from "../../core-module/ui/ui-constants";
import {RestNetworkService} from "../../core-module/rest/services/rest-network.service";

/**
 * create a route to a given node
 * The method currently supports folders (workspace), files (render) and collections
 */
@Pipe({name: 'nodeUrl'})
export class NodeUrlPipe implements PipeTransform {
    constructor(private router: Router) {

    }
    transform(item : Node): string {
        let data: any;
        if (NodeHelper.isNodeCollection(item)) {
            data = {
                routerLink: [UIConstants.ROUTER_PREFIX + 'collections'],
                queryParams: { id: item.ref.id },
            };
        } else {
            if (item.isDirectory) {
                data = {
                    routerLink: [UIConstants.ROUTER_PREFIX + 'workspace'],
                    queryParams: { id: item.ref.id },
                };
            } else {
                let fromeHome = RestNetworkService.isFromHomeRepo(item);
                data = {
                    routerLink: [
                        UIConstants.ROUTER_PREFIX + 'render/' + item.ref.id,
                    ],
                    queryParams: {
                        repository: fromeHome ? null : item.ref.repo,
                    },
                };
            }
        }
        let url = this.router
            .createUrlTree(data.routerLink, { queryParams: data.queryParams })
            .toString();
        // use relative url to make base-href work properly
        if (url.startsWith('/')) url = url.substring(1);
        return url;
    }
}
