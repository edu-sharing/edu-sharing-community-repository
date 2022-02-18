import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import { Node, RestNodeService, RestConstants } from '../../../core-module/core.module';
import { DragData, DropData } from '../../directives/drag-nodes/drag-nodes';
import { Params, QueryParamsHandling } from '@angular/router';

/**
 * Breadcrumbs for nodes or collections.
 */
@Component({
    selector: 'breadcrumbs',
    templateUrl: 'breadcrumbs.component.html',
    styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent {
    /**
     * Caption of the home, if not set, an icon is used.
     */
    @Input() home: string;

    /**
     * shall an invisbile description (for screen readers) be generated, similar to
     * 'You're here'
     */
    @Input() invisibleDescription = false;
    /**
     * The path to give to `routerLink` on the home element.
     *
     * If not given, clicks will be emitted via the `onClick` output.
     */
    @Input() homeRouterLink: {
        routerLink: any[] | string;
        queryParams?: Params | null;
        queryParamsHandling?: QueryParamsHandling | null
    };
    /**
     * Attach a clickable class so the user cursor will be a hand.
     */
    @Input() clickable = true;
    /**
     * Show a short variant (only the last item).
     *
     * `auto` (default) decides via media query.
     */
    @HostBinding('attr.short')
    @Input() short: 'never' | 'always' | 'auto' = 'auto';
    /**
     * Should automatically be linked via angular routing.
     *
     * If set true, the onClick emitter will only be fired for the "root" element.
     */
    @Input() createLink = true;
    /**
     * Allow Dropping of other items (nodes) on to the breadcrumb items.
     *
     * A function that should return true or false and gets the same argument object as the onDrop
     * callback.
     */
    @Input() canDrop: (arg0: DropData) => boolean = (arg0: DropData) => {
        return false;
    };
    /**
     * Set a search query so the breadcrumbs will show this query.
     */
    @Input() set searchQuery(searchQuery: string) {
        this._searchQuery = searchQuery;
        this.addSearch();
    }
    /**
     * Set the breadcrumb list as a @Node array.
     */
    @Input() set breadcrumbsAsNode(nodes: Node[]) {
        if (nodes == null) return;
        this.nodes = nodes;
        this.addSearch();
    }
    /**
     * Set the breadcrumb main id.
     *
     * The breadcrumb nodes will get async resolved via API.
     */
    @Input() set breadcrumbsForId(id: string) {
        if (id == null) return;
        this.node.getNodeParents(id, false, [RestConstants.ALL]).subscribe(nodes => {
            this.nodes = nodes.nodes.reverse();
            this.addSearch();
        });
    }

    /**
     * A breadcrumb is clicked.
     *
     * Passes the index **starting at 1** of the clicked breadcrumb, or 0 for the root element.
     */
    @Output() onClick = new EventEmitter<number>();
    /**
     * Called when an item is dropped on the breadcrumbs.
     */
    @Output() onDrop = new EventEmitter();

    nodes: Node[] = [];
    dragHover: Node;

    private _searchQuery: string;

    constructor(private node: RestNodeService) {}

    canDropNodes(target: Node, { event, nodes, dropAction }: DragData) {
        return this.canDrop({ event, nodes, dropAction, target });
    }

    onNodesHoveringChange(nodesHovering: boolean, target: Node) {
        if (nodesHovering) {
            this.dragHover = target;
        } else {
            // The enter event of another node might have fired before this leave
            // event and already updated `dragHover`. Only set it to null if that is
            // not the case.
            if (this.dragHover === target) {
                this.dragHover = null;
            }
        }
    }

    onNodesDrop({ event, nodes, dropAction }: DragData, target: Node) {
        if (dropAction === 'link') {
            throw new Error('dropAction "link" is not allowed');
        }
        this.onDrop.emit({
            target,
            source: nodes,
            event,
            type: dropAction,
        });
    }

    openBreadcrumb(position: number) {
        this.onClick.emit(position);
    }

    private addSearch() {
        const add = !(
            this.nodes.length > 0 &&
            this.nodes[this.nodes.length - 1] &&
            this.nodes[this.nodes.length - 1].type === 'SEARCH'
        );
        if (this._searchQuery) {
            const search = new Node();
            search.name = `'${this._searchQuery}'`;
            search.type = 'SEARCH';
            if (add) {
                this.nodes.splice(this.nodes.length, 0, search);
            } else {
                this.nodes[this.nodes.length - 1] = search;
            }
        } else if (!add) {
            this.nodes.splice(this.nodes.length, 1);
        }
    }
}
