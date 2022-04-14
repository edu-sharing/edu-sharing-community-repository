import { trigger } from '@angular/animations';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { Node, NodeListService, SortPolicy } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ActionbarHelperService } from '../../../common/services/actionbar-helper';
import { DialogButton, ListItem, RestConstants } from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { CustomOptions, DefaultGroups, OptionItem } from '../../../core-ui-module/option-item';
import { Toast } from '../../../core-ui-module/toast';

@Component({
    selector: 'es-search-node-store',
    templateUrl: 'node-store.component.html',
    styleUrls: ['node-store.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class SearchNodeStoreComponent implements OnInit {
    @Output() onClose = new EventEmitter();

    selected: Node[] = [];
    columns: ListItem[] = [];
    options: CustomOptions = {
        useDefaultOptions: false,
    };
    buttons = DialogButton.getSingleButton('CLOSE', () => this.cancel(), DialogButton.TYPE_CANCEL);
    loading = true;
    nodes: Node[] = [];

    readonly sortPolicySubject = new BehaviorSubject<SortPolicy>({
        property: RestConstants.LOM_PROP_TITLE,
        direction: 'asc',
    });

    constructor(
        private toast: Toast,
        private router: Router,
        private actionbar: ActionbarHelperService,
        private nodeHelper: NodeHelperService,
        private nodeList: NodeListService,
    ) {
        this.columns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
    }

    ngOnInit(): void {
        this.loading = true;
        this.registerNodeList();
    }

    onDoubleClick(node: Node) {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id], {
            state: {
                nodes: this.nodes,
                scope: 'node-store',
            },
        });
    }

    onSelection(data: Node[]) {
        this.selected = data;
        this.updateActionOptions();
    }

    cancel() {
        this.onClose.emit();
    }

    changeSort(data: any) {
        this.sortPolicySubject.next({
            property: data.sortBy,
            direction: data.sortAscending ? 'asc' : 'desc',
        });
    }

    private updateActionOptions() {
        this.options.addOptions = [];
        const download = this.actionbar.createOptionIfPossible(
            'DOWNLOAD',
            this.selected,
            (node: Node) => this.nodeHelper.downloadNodes(node ? [node] : this.selected),
        );
        download.group = DefaultGroups.FileOperations;
        this.options.addOptions.push(download);
        const remove = new OptionItem('SEARCH.NODE_STORE.REMOVE_ITEM', 'delete', () => {
            this.deleteSelection();
        });
        remove.group = DefaultGroups.FileOperations;
        this.options.addOptions.push(remove);
    }

    private deleteSelection() {
        this.loading = true;
        this.nodeList
            .removeFromNodeList(
                RestConstants.NODE_STORE_LIST,
                this.selected.map((node) => node.ref.id),
            )
            .subscribe(() =>
                this.toast.toast('SEARCH.NODE_STORE.REMOVED_ITEMS', {
                    count: this.selected.length,
                }),
            );
    }

    private registerNodeList(): void {
        this.sortPolicySubject
            .pipe(
                switchMap((sortPolicy) =>
                    this.nodeList.observeNodeList(RestConstants.NODE_STORE_LIST, {
                        propertyFilter: [RestConstants.ALL],
                        sortPolicies: [sortPolicy],
                    }),
                ),
            )
            .subscribe((nodes) => {
                this.nodes = nodes.nodes;
                this.loading = false;
                this.updateActionOptions();
            });
    }
}
