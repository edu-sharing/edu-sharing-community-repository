import { Overlay } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable, Injector } from '@angular/core';
import { Node } from '../../../core-module/rest/data-object';
import { NodeEmbedComponent, NODE_EMBED_CONFIG } from './node-embed.component';

@Injectable({
    providedIn: 'root',
})
export class NodeEmbedService {
    constructor(private overlay: Overlay, private injector: Injector) {}

    open(node: Node): void {
        const overlayRef = this.overlay.create();
        const injector = Injector.create({
            parent: this.injector,
            providers: [
                {
                    provide: NODE_EMBED_CONFIG,
                    useValue: { node, onClose: () => overlayRef.dispose() },
                },
            ],
        });
        const portal = new ComponentPortal(NodeEmbedComponent, undefined, injector);
        overlayRef.attach(portal);
    }
}
