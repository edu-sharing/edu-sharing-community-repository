import { Component, OnInit, Input } from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {NativeWidget} from '../../mds-editor-view/mds-editor-view.component';

@Component({
    selector: 'app-mds-editor-widget-link',
    templateUrl: './mds-editor-widget-link.component.html',
    styleUrls: ['./mds-editor-widget-link.component.scss'],
})
export class MdsEditorWidgetLinkComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    @Input() widgetName: 'license' | 'template';

    hasChanges = new BehaviorSubject<boolean>(false);

    // caption: string; // Could use as label.
    linkLabel: string;

    constructor() {}

    ngOnInit(): void {
        switch (this.widgetName) {
            case 'license':
                // this.caption = 'MDS.LICENSE';
                this.linkLabel = 'MDS.LICENSE_LINK';
                break;
            case 'template':
                this.linkLabel = 'MDS.TEMPLATE_LINK';
                break;
        }
    }

    onClick(): void {
        throw new Error('not implemented');
    }
}
