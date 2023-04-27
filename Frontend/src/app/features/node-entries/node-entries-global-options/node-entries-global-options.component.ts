import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    OnDestroy,
} from '@angular/core';
import { NodeEntriesDataType } from '../node-entries.component';
import { NodeEntriesService } from '../../../core-ui-module/node-entries.service';
import { Node } from 'ngx-edu-sharing-api';
import { NodeEntriesDisplayType } from '../entries-model';

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
