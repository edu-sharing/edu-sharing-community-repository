import { Component, inject } from '@angular/core';
import { combineLatest, map } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';
import { ListItem } from '../../types/list-item';
import { ListWidget } from '../list-widget';
import { NodeHelperService } from '../../services/node-helper.service';
import { AccessibilityService } from '../../services/accessibility.service';
import { LocalEventsService } from '../../services/local-events.service';

@Component({
    selector: 'es-list-collection-info',
    templateUrl: './list-collection-info.component.html',
    styleUrls: ['./list-collection-info.component.scss'],
    standalone: false,
})
export class ListCollectionInfoComponent extends ListWidget {
    private accessibility = inject(AccessibilityService);
    nodeHelper = inject(NodeHelperService);
    private localEvents = inject(LocalEventsService);

    static supportedItems = [
        new ListItem('COLLECTION', 'info'),
        new ListItem('COLLECTION', 'scope'),
    ];

    readonly indicatorIcons$;
    readonly nodeModified$;

    constructor() {
        super();
        this.indicatorIcons$ = this.accessibility.observe('indicatorIcons');
        this.nodeModified$ = combineLatest([
            this.nodeSubject,
            this.localEvents.createdNodes$,
            this.localEvents.changedNodes$,
        ]).pipe(
            map(([node, created, changed]) => {
                const id = (node as Node)?.ref?.id;
                return (
                    !!id &&
                    [...created, ...changed].some(
                        (n) =>
                            n.ref?.id === id ||
                            // since count info propagates downwards also check parent
                            n.parent?.id === id,
                    )
                );
            }),
        );
    }
}
