import { Component, OnInit } from '@angular/core';
import {NativeWidgetComponent} from '../../mds-editor-view/mds-editor-view.component';
import {BehaviorSubject} from 'rxjs';

@Component({
    selector: 'app-mds-editor-widget-version',
    templateUrl: './mds-editor-widget-version.component.html',
    styleUrls: ['./mds-editor-widget-version.component.scss'],
})
export class MdsEditorWidgetVersionComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);

    comment: string;
    file: File;

    constructor() {}

    ngOnInit(): void {}

    onChange(): void {
        this.updateState();
    }

    setFile(event: Event) {
        this.file = (event.target as HTMLInputElement).files?.[0];
        this.updateState();
    }

    private updateState() {
        this.hasChanges.next(!!this.comment || !!this.file);
    }
}
