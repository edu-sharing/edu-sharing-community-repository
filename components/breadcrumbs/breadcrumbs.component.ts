import { Component, Input, Output, EventEmitter } from '@angular/core';
import { UIHelper } from '../../ui-helper';
import {
    NodeList,
    Node,
    RestNodeService,
    TemporaryStorageService,
    UIService,
} from '../../../core-module/core.module';
import { Router } from '@angular/router';

/**
 * A module that provides breadcrumbs for nodes or collections
 */
@Component({
    selector: 'breadcrumbs',
    templateUrl: 'breadcrumbs.component.html',
    styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent {
    /**
     * Caption of the home, if not set, an icon is used
     */
    @Input() home: string;
    /**
     * Attach a clickable class so the user cursor will be a hand
     */
    @Input() clickable = true;
    /**
     * Show a short variant (only the last item)
     * auto (default) decides via media query
     * Also possible: never, always
     */
    @Input() short = 'auto';
    /**
     * Allow Dropping of other items (nodes) on to the breadcrumb items
     * A function that should return true or false and gets the same argument object as the onDrop callback
     */
    @Input() canDrop: Function = () => {
        return false;
    };
    /**
     * Set the id of the parent where all sub-nodes are currently in, e.g. SHARED_FILES
     */
    @Input() set homeId(homeId: string) {
        if (!homeId) return;
        this.node.getChildren(homeId).subscribe((data: NodeList) => {
            this.mainParents = data.nodes;
        });
    }
    /**
     * Set a search query so the breadcrumbs will show this query
     */
    @Input() set searchQuery(searchQuery: string) {
        this._searchQuery = searchQuery;
        this.addSearch();
    }
    /**
     * Set the breadcrumb list as a @Node array
     */
    @Input() set breadcrumbsAsNode(nodes: Node[]) {
        if (nodes == null) return;
        this._breadcrumbsAsNode = nodes;
        this.generateShort();
    }
    /**
     * Set the breadcrumb main id
     * The breadcrumb nodes will get async resolved via api
     */
    @Input() set breadcrumbsForId(id: string) {
        if (id == null) return;
        this.node.getNodeParents(id).subscribe(nodes => {
            this._breadcrumbsAsNode = nodes.nodes.reverse();
            this.generateShort();
        });
    }

    @Output() onClick = new EventEmitter();
    /**
     * Called when an item is dropped on the breadcrumbs
     *
     * @type {EventEmitter<any>}
     */
    @Output() onDrop = new EventEmitter();

    _breadcrumbsAsNode: Node[] = [];
    _breadcrumbsAsNodeShort: Node[] = [];

    private _breadcrumbsAsId: string[];
    private dragHover: Node;
    private _searchQuery: string;
    private isBuilding = false;
    private mainParents: Node[];

    constructor(
        private node: RestNodeService,
        private router: Router,
        private storage: TemporaryStorageService,
        private ui: UIService,
    ) {}

    generateUrl(node: Node) {
        return UIHelper.createUrlToNode(this.router, node);
    }

    private openBreadcrumb(position: number) {
        this.onClick.emit(position);
        return false;
    }

    private allowDrag(event: any, target: Node) {
        if (
            UIHelper.handleAllowDragEvent(
                this.storage,
                this.ui,
                event,
                target,
                this.canDrop,
            )
        ) {
            this.dragHover = target;
        }
    }

    private drop(event: any, target: Node) {
        this.dragHover = null;
        UIHelper.handleDropEvent(
            this.storage,
            this.ui,
            event,
            target,
            this.onDrop,
        );
    }

    private generateShort() {
        this.addSearch();
        if (this._breadcrumbsAsNode.length < 2)
            this._breadcrumbsAsNodeShort = this._breadcrumbsAsNode.slice();
        else
            this._breadcrumbsAsNodeShort = this._breadcrumbsAsNode.slice(
                this._breadcrumbsAsNode.length - 2,
            );
    }

    private addSearch() {
        let add = !(
            this._breadcrumbsAsNode.length > 0 &&
            this._breadcrumbsAsNode[this._breadcrumbsAsNode.length - 1] &&
            this._breadcrumbsAsNode[this._breadcrumbsAsNode.length - 1].type ==
                'SEARCH'
        );
        if (this._searchQuery) {
            let search = new Node();
            search.name = "'" + this._searchQuery + "'";
            search.type = 'SEARCH';
            if (add) {
                this._breadcrumbsAsNode.splice(
                    this._breadcrumbsAsNode.length,
                    0,
                    search,
                );
            } else {
                this._breadcrumbsAsNode[
                    this._breadcrumbsAsNode.length - 1
                ] = search;
            }
        } else if (!add) {
            this._breadcrumbsAsNode.splice(this._breadcrumbsAsNode.length, 1);
        }
    }

    private parentContains(id: String) {
        for (let node of this.mainParents) {
            if (node.ref.id == id) return true;
        }
        return false;
    }
}
