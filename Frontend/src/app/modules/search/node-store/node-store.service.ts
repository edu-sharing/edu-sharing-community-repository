import { Injectable } from '@angular/core';
import { Node, NodeListErrorResponses, NodeListService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Toast } from 'src/app/core-ui-module/toast';
import { RestConstants, RestHelper } from '../../../core-module/core.module';

@Injectable({
    providedIn: 'root',
})
export class NodeStoreService {
    constructor(private nodeList: NodeListService, private toast: Toast) {}

    add(nodes: Node[]): Observable<void> {
        return this.nodeList
            .addToNodeList(
                RestConstants.NODE_STORE_LIST,
                nodes.map((node) => node.ref.id),
            )
            .pipe(
                tap(() => {
                    this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                        count: nodes.length,
                    });
                }),
                catchError((errors: NodeListErrorResponses) => {
                    const numberSuccessful = nodes.length - errors.length;
                    if (numberSuccessful > 0) {
                        this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                            count: numberSuccessful,
                        });
                    }
                    for (const { nodeId, error } of errors) {
                        if (RestHelper.errorMessageContains(error, 'Node is already in list')) {
                            this.toast.error(null, 'SEARCH.ADDED_TO_NODE_STORE_EXISTS', {
                                name: RestHelper.getTitle(
                                    nodes.find((node) => node.ref.id === nodeId),
                                ),
                            });
                            error.preventDefault();
                        }
                    }
                    return rxjs.of(void 0);
                }),
            );
    }
}
