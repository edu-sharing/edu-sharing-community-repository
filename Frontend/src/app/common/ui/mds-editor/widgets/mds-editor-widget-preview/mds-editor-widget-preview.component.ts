import { Component, OnInit } from '@angular/core';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import {NativeWidget} from '../../mds-editor-view/mds-editor-view.component';
import {BehaviorSubject} from 'rxjs';

@Component({
    selector: 'app-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
})
export class MdsEditorWidgetPreviewComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);
    src: string;
    constructor(private mdsEditorValues: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes.subscribe((nodes) => {
            if (nodes?.length === 1) {
                this.src = nodes[0].preview.url + '&crop=true&width=400&height=300';
            }
        });
    }
}
