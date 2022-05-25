import { Overlay } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable } from '@angular/core';
import { Node, NodeListErrorResponses, NodeListService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Toast } from 'src/app/core-ui-module/toast';
import { RestConstants, RestHelper } from '../../../core-module/core.module';
import { SearchNodeStoreComponent } from './node-store.component';

@Injectable({
    providedIn: 'root',
})
export class NodeStoreService {
    constructor(
        private nodeList: NodeListService,
        private overlay: Overlay,
        private toast: Toast,
    ) {}

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

    open(onDestroy?: () => void): void {
        const overlayRef = this.overlay.create();
        const portal = new ComponentPortal(SearchNodeStoreComponent);
        const componentRef = overlayRef.attach(portal);
        componentRef.instance.onClose.subscribe(() => overlayRef.dispose());
        if (onDestroy) {
            componentRef.onDestroy(onDestroy);
        }
    }
}
