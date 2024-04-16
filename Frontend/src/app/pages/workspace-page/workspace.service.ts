import { EventEmitter, Injectable } from '@angular/core';
import { Node } from '../../core-module/core.module';

/**
 * Passes on workspace properties and functions for outside use.
 */
@Injectable({
    providedIn: 'root',
})
export class WorkspaceService {
    nodeSidebar: Node;
    nodeSidebarChange = new EventEmitter<Node>();
}
