import { Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import { BehaviorSubject } from 'rxjs';
import { PreviewSidebarComponent } from './preview-sidebar.component';
import { BridgeService } from '../../../services/bridge.service';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { NodeHelperService } from '../../../services/node-helper.service';

/**
 * Sidebar component that previews an element with preview image and some metadata.
 *
 * The sidebar will collapse and display as overlay dialog instead on small screens.
 */
@Injectable({
    providedIn: 'root',
})
export class PreviewSidebarService {
    private currentNode$ = new BehaviorSubject<Node | null>(null);
    private instance$ = new BehaviorSubject<PreviewSidebarComponent>(null);
    private sidebarOpen$ = new BehaviorSubject<boolean>(false);

    constructor(private bridgeService: BridgeService, private nodeHelper: NodeHelperService) {}

    registerInstance(instance: PreviewSidebarComponent) {
        if (this.instance$.value) {
            console.warn(
                'Multiple sidebar registration detected! Check your DOM',
                this.instance$.value,
                instance,
            );
        }
        this.instance$.next(instance);
        // the currentNode might already be set as input
        this.currentNode$.next(instance.node);
        // the sidebar is open if the node is set
        this.sidebarOpen$.next(!!instance.node);
        // listen to the closed event
        this.instance$.value.closed.subscribe(() => {
            void this.instance$.value.updateNode(null);
            this.currentNode$.next(null);
            this.sidebarOpen$.next(false);
        });
    }

    unregisterInstance(instance: PreviewSidebarComponent) {
        if (this.instance$.value !== instance) {
            console.error('This sidebar is not registered!', this.instance$.value, instance);
        } else {
            this.instance$.next(null);
        }
    }

    getInstance() {
        return this.instance$;
    }

    getCurrentNode() {
        return this.currentNode$;
    }

    getOpenState() {
        return this.sidebarOpen$;
    }

    handleNodeClick(node: Node) {
        if (!this.instance$.value) {
            UIHelper.openUrl(
                this.nodeHelper.getNodeUrl(node),
                this.bridgeService,
                OPEN_URL_MODE.Blank,
            );
            return;
        }
        const newNode = this.instance$.value.node !== node ? node : null;
        this.currentNode$.next(newNode);
        this.sidebarOpen$.next(!!newNode);
        void this.instance$.value.updateNode(newNode);
    }
}
