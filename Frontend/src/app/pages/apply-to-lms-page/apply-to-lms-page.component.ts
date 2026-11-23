import { Component } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    FrameEventsService,
    Node,
    NodeRemoteWrapper,
    NodeWrapper,
    RestConnectorService,
    RestConstants,
    RestLocatorService,
    RestNodeService,
    TemporaryStorageService,
} from '../../core-module/core.module';
import { Toast } from '../../services/toast';
import { NodeHelperService } from '../../services/node-helper.service';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { PlatformLocation } from '@angular/common';

export class NodeLMS extends Node {
    objectUrl?: string;
    nodeId?: string;
}

@Component({
    selector: 'es-apply-to-lms-page',
    templateUrl: 'apply-to-lms-page.component.html',
    standalone: false,
})
export class ApplyToLmsPageComponent {
    constructor(
        private connector: RestConnectorService,
        private locator: RestLocatorService,
        private nodeApi: RestNodeService,
        private toast: Toast,
        private events: FrameEventsService,
        private translations: TranslationsService,
        private temporaryStorage: TemporaryStorageService,
        private nodeHelper: NodeHelperService,
        private router: Router,
        private platformLocation: PlatformLocation,
        private route: ActivatedRoute,
    ) {
        this.route.queryParams.subscribe((params: Params) => {
            if (params.reurl) {
                this.reurl = params.reurl;
            }
            this.route.params.subscribe((params: Params) => {
                this.toast.showProgressSpinner();
                if (temporaryStorage.get(TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE)) {
                    this.node = temporaryStorage.get(
                        TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE,
                    );
                    this.forward();
                } else if (params.node) {
                    this.nodeApi
                        .getNodeMetadata(params.node, [RestConstants.ALL], params.repo)
                        .subscribe(
                            (data: NodeWrapper) => {
                                this.node = data.node;
                                this.forward();
                            },
                            (error: any) => {
                                this.translations.waitForInit().subscribe(() => {
                                    this.toast.error(error);
                                });
                            },
                        );
                }
            });
        });
    }
    node: Node;
    reurl: string;

    forward() {
        this.nodeApi.prepareUsage(this.node.ref.id, this.node.ref.repo).subscribe(
            (nodeWrapper) => {
                this.applyNode(nodeWrapper);
            },
            (error) => {
                this.toast.error(error);
            },
        );
    }

    private applyNode(wrapper: NodeRemoteWrapper) {
        const node: NodeLMS = wrapper.node;
        // copy the main object to remote (in this case, it's simply a regular, local object)
        if (!wrapper.remote) wrapper.remote = wrapper.node;
        // only frame communication is supported, absolute urls (redirects) are no longer allowed
        if (this.reurl !== 'IFRAME' && this.reurl !== 'WINDOW') {
            console.error(
                'Unsupported value for the "reurl" parameter. Only "IFRAME" or "WINDOW" are supported, please use frame communication',
            );
            this.toast.error(null);
            return;
        }
        // the ccrep should always point to the local object (relevant if it's from a remote repo)
        node.objectUrl =
            'ccrep://' +
            encodeURIComponent(wrapper.remote.ref.repo) +
            '/' +
            encodeURIComponent(wrapper.remote.ref.id);
        node.nodeId = wrapper.remote.ref.id;
        this.nodeHelper.appendImageData(node).then(
            (data) => {
                this.events.broadcastEvent(FrameEventsService.EVENT_APPLY_NODE, data);
                window.history.back();
            },
            (error) => {
                console.warn('failed to fetch image data', error);
                this.events.broadcastEvent(FrameEventsService.EVENT_APPLY_NODE, node);
                window.history.back();
            },
        );
    }
}
