import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeEntriesDisplayType } from '../entries-model';
import { NodeEntriesService } from '../../services/node-entries.service';

@Component({
    selector: 'es-node-entries-global-options',
    templateUrl: './node-entries-global-options.component.html',
    styleUrls: ['./node-entries-global-options.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEntriesGlobalOptionsComponent<T extends Node> {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    @Input() displayType: NodeEntriesDisplayType;
    constructor(public entriesService: NodeEntriesService<T>) {}
}
