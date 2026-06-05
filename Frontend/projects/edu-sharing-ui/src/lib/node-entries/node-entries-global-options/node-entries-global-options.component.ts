import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeEntriesDisplayType } from '../entries-model';
import { NodeEntriesService } from '../../services/node-entries.service';
import { map } from 'rxjs/operators';

@Component({
    selector: 'es-node-entries-global-options',
    templateUrl: './node-entries-global-options.component.html',
    styleUrls: ['./node-entries-global-options.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodeEntriesGlobalOptionsComponent<T extends Node> {
    entriesService = inject<NodeEntriesService<T>>(NodeEntriesService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @Input() displayType: NodeEntriesDisplayType;

    getEnabledOptions() {
        return this.entriesService.globalOptionsSubject.pipe(
            map((options) => options.filter((e) => e.isEnabled)),
        );
    }
}
