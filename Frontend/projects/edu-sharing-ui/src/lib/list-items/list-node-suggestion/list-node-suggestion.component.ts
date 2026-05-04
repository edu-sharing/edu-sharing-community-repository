import { Component, OnInit } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import * as Constants from 'ngx-edu-sharing-api';
import { MdsService, NodeSuggestion } from 'ngx-edu-sharing-api';
import { MdsHelperService } from '../../mds/mds-helper.service';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-list-node-suggestion',
    templateUrl: './list-node-suggestion.component.html',
    styleUrls: ['./list-node-suggestion.component.scss'],
    standalone: false,
})
export class ListNodeSuggestionComponent extends ListWidget implements OnInit {
    static supportedItems = [new ListItem('SUGGESTION', '*')];

    readonly pendingCount$ = new BehaviorSubject<number>(0);
    readonly pendingPropertyCaptions$ = new BehaviorSubject<string[]>([]);

    constructor(private mds: MdsService) {
        super();
    }

    async ngOnInit() {
        this.nodeSubject.subscribe(async (node: NodeSuggestion) => {
            if (!node) {
                return;
            }
            const pending = (node.suggestions ?? []).filter((s) => s.status === 'PENDING');
            this.pendingCount$.next(pending.length);
            const ids = [...new Set(pending.map((s) => s.propertyId))];
            try {
                const mdsSet = await this.mds
                    .getMetadataSet({
                        repository: node.ref?.repo || Constants.HOME_REPOSITORY,
                        metadataSet: node.metadataset || Constants.DEFAULT,
                    })
                    .toPromise();
                this.pendingPropertyCaptions$.next(
                    ids.map(
                        (id) => MdsHelperService.getWidget(id, null, mdsSet.widgets)?.caption ?? id,
                    ),
                );
            } catch {
                this.pendingPropertyCaptions$.next(ids);
            }
        });
    }
}
