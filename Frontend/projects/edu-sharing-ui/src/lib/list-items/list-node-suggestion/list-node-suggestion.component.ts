import { Component, OnInit, inject } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import { MdsService, NodeSuggestion } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-list-node-suggestion',
    templateUrl: './list-node-suggestion.component.html',
    styleUrls: ['./list-node-suggestion.component.scss'],
    standalone: false,
})
export class ListNodeSuggestionComponent extends ListWidget implements OnInit {
    private mds = inject(MdsService);

    static supportedItems = [new ListItem('SUGGESTION', '*')];

    readonly pendingCount$ = new BehaviorSubject<number>(0);

    async ngOnInit() {
        this.nodeSubject.subscribe(async (node: NodeSuggestion) => {
            if (!node) {
                return;
            }
            const pending = (node.suggestions ?? []).filter((s) => s.status === 'PENDING');
            this.pendingCount$.next(pending.length);
            const ids = [...new Set(pending.map((s) => s.propertyId))];
        });
    }
}
