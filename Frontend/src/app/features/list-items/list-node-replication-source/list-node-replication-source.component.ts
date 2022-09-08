import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { AccessibilityService } from 'src/app/common/ui/accessibility/accessibility.service';
import { ListItem, RestConstants, Node, RestNetworkService } from 'src/app/core-module/core.module';
import { ListWidget } from '../list-widget';
import { NodeSourcePipe } from '../node-source.pipe';

@Component({
    selector: 'es-list-node-replication-source',
    templateUrl: './list-node-replication-source.component.html',
    providers: [NodeSourcePipe],
})
export class ListNodeReplicationSourceComponent extends ListWidget {
    static supportedItems = [new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE)];

    readonly replicationSource$ = this.nodeSubject.pipe(
        map((node) => (node as Node).properties['ccm:replicationsource']),
    );

    readonly text$ = this.replicationSource$.pipe(
        // Wait for repositories to be available to `restNetwork` since `nodeSource.transform` fails
        // if it isn't.
        switchMap((replicationSource) =>
            this.restNetwork.getRepositories().pipe(map(() => replicationSource)),
        ),
        switchMap((replicationSource) =>
            this.translate.get(
                'REPOSITORIES.' + this.nodeSource.transform(replicationSource, { mode: 'escaped' }),
                { fallback: this.nodeSource.transform(replicationSource, { mode: 'text' }) },
            ),
        ),
    );

    readonly tooltip$ = rxjs.combineLatest([this.text$, this.provideLabelSubject]).pipe(
        switchMap(([text, provideLabel]) => {
            if (provideLabel) {
                return this.translate
                    .get('NODE.ccm:replicationsource')
                    .pipe(map((replicationSource) => `${replicationSource}: ${text}`));
            } else {
                return rxjs.of(text);
            }
        }),
    );

    readonly indicatorIcons$ = this.accessibility.observe('indicatorIcons');

    constructor(
        private accessibility: AccessibilityService,
        private nodeSource: NodeSourcePipe,
        private translate: TranslateService,
        private restNetwork: RestNetworkService,
    ) {
        super();
    }
}
